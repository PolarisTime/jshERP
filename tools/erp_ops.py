#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""jshERP 数据库运维工具 — 交互式 CLI"""

import sys
import pymysql
from pymysql.cursors import DictCursor

# ─── 数据库配置 ──────────────────────────────────────────────────
DB_CONFIG = dict(
    host='127.0.0.1',
    port=3306,
    user='jsh_steel',
    password='jshM0mC6vU8mI4hA4ksteel',
    database='jsh_erp_steel',
    charset='utf8mb4',
    cursorclass=DictCursor,
)

# ─── ANSI 颜色 ──────────────────────────────────────────────────
class C:
    RESET  = '\033[0m'
    BOLD   = '\033[1m'
    RED    = '\033[91m'
    GREEN  = '\033[92m'
    YELLOW = '\033[93m'
    CYAN   = '\033[96m'
    GRAY   = '\033[90m'

def color(text, c):
    return f'{c}{text}{C.RESET}'

def title(text):
    print(f'\n{C.BOLD}{C.CYAN}{"═" * 60}{C.RESET}')
    print(f'{C.BOLD}{C.CYAN}  {text}{C.RESET}')
    print(f'{C.BOLD}{C.CYAN}{"═" * 60}{C.RESET}')

def warn(text):
    print(color(f'⚠ {text}', C.YELLOW))

def ok(text):
    print(color(f'✓ {text}', C.GREEN))

def err(text):
    print(color(f'✗ {text}', C.RED))

def info(text):
    print(color(f'  {text}', C.GRAY))

# ─── 数据库操作封装 ──────────────────────────────────────────────
def get_conn():
    return pymysql.connect(**DB_CONFIG)

def query(sql, args=None):
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args)
            return cur.fetchall()
    finally:
        conn.close()

def execute(sql, args=None):
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            rows = cur.execute(sql, args)
            conn.commit()
            return rows
    finally:
        conn.close()

def confirm(prompt='确认执行？(y/n): '):
    return input(color(prompt, C.YELLOW)).strip().lower() in ('y', 'yes', '是')

def input_numbers(prompt='请输入单据号（逗号分隔）: '):
    raw = input(prompt).strip()
    return [n.strip() for n in raw.split(',') if n.strip()]

def print_table(rows, keys=None):
    if not rows:
        info('(无数据)')
        return
    if keys is None:
        keys = list(rows[0].keys())
    widths = {}
    for k in keys:
        widths[k] = max(len(str(k)), max(len(str(r.get(k, ''))) for r in rows))
    header = ' | '.join(str(k).ljust(widths[k]) for k in keys)
    print(f'  {C.BOLD}{header}{C.RESET}')
    print(f'  {"-+-".join("-" * widths[k] for k in keys)}')
    for r in rows:
        line = ' | '.join(str(r.get(k, '')).ljust(widths[k]) for k in keys)
        print(f'  {line}')

# ─── 1. 诊断单据 ────────────────────────────────────────────────
def cmd_diagnose():
    title('诊断单据')
    numbers = input_numbers()
    if not numbers:
        return
    placeholders = ','.join(['%s'] * len(numbers))

    # 主表
    heads = query(f'''
        SELECT number, type, sub_type, status, organ_id, link_number,
               discount_last_money, other_money, deposit, change_amount,
               total_price, linked_flag, delete_flag
        FROM jsh_depot_head
        WHERE number IN ({placeholders}) AND ifnull(delete_flag,'0')!='1'
    ''', numbers)
    if not heads:
        # 尝试物流单
        freight = query(f'''
            SELECT bill_no, status, total_weight, total_freight, delete_flag
            FROM jsh_freight_head
            WHERE bill_no IN ({placeholders}) AND ifnull(delete_flag,'0')!='1'
        ''', numbers)
        if freight:
            print(f'\n  {C.BOLD}物流单主表{C.RESET}')
            print_table(freight)
            for f in freight:
                items = query('''
                    SELECT fi.depot_head_id, fi.depot_number, fi.weight,
                           dh.number as xsck_number, dh.link_number
                    FROM jsh_freight_item fi
                    LEFT JOIN jsh_depot_head dh ON dh.id = fi.depot_head_id
                    WHERE fi.header_id = (SELECT id FROM jsh_freight_head WHERE bill_no=%s AND ifnull(delete_flag,'0')!='1')
                      AND ifnull(fi.delete_flag,'0')!='1'
                ''', (f['bill_no'],))
                print(f'\n  {C.BOLD}物流单明细{C.RESET}')
                print_table(items)
        else:
            err('未找到单据')
        return

    print(f'\n  {C.BOLD}主表信息{C.RESET}')
    print_table(heads)

    for h in heads:
        num = h['number']
        items = query('''
            SELECT di.id, di.material_id, m.name as material, di.oper_number,
                   di.unit_price, di.all_price, di.tax_last_money, di.weight, di.link_id
            FROM jsh_depot_item di
            LEFT JOIN jsh_material m ON m.id = di.material_id
            WHERE di.header_id = (SELECT id FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,'0')!='1')
              AND ifnull(di.delete_flag,'0')!='1'
        ''', (num,))
        print(f'\n  {C.BOLD}明细 [{num}]{C.RESET}')
        print_table(items)

        # 关联链路
        if h.get('link_number'):
            links = h['link_number'].split(',')
            lk_placeholders = ','.join(['%s'] * len(links))
            linked = query(f'''
                SELECT number, type, sub_type, status, discount_last_money
                FROM jsh_depot_head
                WHERE number IN ({lk_placeholders}) AND ifnull(delete_flag,'0')!='1'
            ''', links)
            print(f'\n  {C.BOLD}关联单据 (link_number){C.RESET}')
            print_table(linked)

        # 被谁引用
        refs = query('''
            SELECT number, type, sub_type
            FROM jsh_depot_head
            WHERE FIND_IN_SET(%s, link_number) > 0 AND ifnull(delete_flag,'0')!='1'
        ''', (num,))
        if refs:
            print(f'\n  {C.BOLD}被引用 (反向关联){C.RESET}')
            print_table(refs)

# ─── 2. 修复优惠后金额 ──────────────────────────────────────────
def cmd_fix_discount_last_money():
    title('修复优惠后金额 (discount_last_money)')
    numbers = input_numbers()
    if not numbers:
        return
    placeholders = ','.join(['%s'] * len(numbers))

    rows = query(f'''
        SELECT h.number, h.discount_last_money as current_val,
               IFNULL(SUM(i.tax_last_money), 0) as correct_val
        FROM jsh_depot_head h
        JOIN jsh_depot_item i ON i.header_id = h.id AND ifnull(i.delete_flag,'0')!='1'
        WHERE h.number IN ({placeholders}) AND ifnull(h.delete_flag,'0')!='1'
        GROUP BY h.id, h.number, h.discount_last_money
    ''', numbers)

    if not rows:
        err('未找到单据')
        return

    need_fix = [r for r in rows if abs(float(r['current_val']) - float(r['correct_val'])) > 0.001]
    if not need_fix:
        ok('所有单据的 discount_last_money 已正确')
        return

    print_table(need_fix, ['number', 'current_val', 'correct_val'])
    if not confirm():
        return

    for r in need_fix:
        execute('''
            UPDATE jsh_depot_head h
            JOIN (SELECT header_id, SUM(tax_last_money) AS s FROM jsh_depot_item
                  WHERE delete_flag!='1' GROUP BY header_id) i ON i.header_id = h.id
            SET h.discount_last_money = i.s
            WHERE h.number = %s AND ifnull(h.delete_flag,'0')!='1'
        ''', (r['number'],))
    ok(f'已修复 {len(need_fix)} 条记录')

# ─── 3. 补全采购订单关联出库 ────────────────────────────────────
def cmd_fill_cgdd_link():
    title('补全采购订单关联出库 (CGDD → XSCK)')
    rows = query('''
        SELECT cgdd.id as cgdd_id, cgdd.number as cgdd_number,
               GROUP_CONCAT(DISTINCT xsck.number ORDER BY xsck.number) as xsck_numbers
        FROM jsh_depot_head cgdd
        JOIN jsh_depot_head cgrk
          ON FIND_IN_SET(cgdd.number, cgrk.link_number) > 0
          AND cgrk.type='入库' AND cgrk.sub_type='采购' AND ifnull(cgrk.delete_flag,'0')!='1'
        JOIN jsh_depot_head xsck
          ON FIND_IN_SET(cgrk.number, xsck.link_number) > 0
          AND xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
        WHERE ifnull(cgdd.delete_flag,'0')!='1'
          AND (cgdd.link_number IS NULL OR cgdd.link_number = '')
        GROUP BY cgdd.id, cgdd.number
    ''')

    if not rows:
        ok('无需补全')
        return

    print_table(rows, ['cgdd_number', 'xsck_numbers'])
    print(f'\n  共 {len(rows)} 条待补全')
    if not confirm():
        return

    for r in rows:
        execute('UPDATE jsh_depot_head SET link_number=%s WHERE id=%s',
                (r['xsck_numbers'], r['cgdd_id']))
    ok(f'已补全 {len(rows)} 条')

# ─── 4. 同步 linked_flag ────────────────────────────────────────
def cmd_sync_linked_flag():
    title('同步 linked_flag 标记')

    # 应标记为1但当前为0的
    should_be_1 = query('''
        SELECT cgrk.id, cgrk.number, cgrk.linked_flag
        FROM jsh_depot_head cgrk
        JOIN jsh_depot_head xsck
          ON FIND_IN_SET(cgrk.number, xsck.link_number) > 0
          AND xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
        WHERE cgrk.type='入库' AND cgrk.sub_type='采购'
          AND ifnull(cgrk.delete_flag,'0')!='1'
          AND ifnull(cgrk.linked_flag,'0') != '1'
        GROUP BY cgrk.id, cgrk.number, cgrk.linked_flag
    ''')

    # 应标记为0但当前为1的
    should_be_0 = query('''
        SELECT cgrk.id, cgrk.number, cgrk.linked_flag
        FROM jsh_depot_head cgrk
        LEFT JOIN jsh_depot_head xsck
          ON FIND_IN_SET(cgrk.number, xsck.link_number) > 0
          AND xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
        WHERE cgrk.type='入库' AND cgrk.sub_type='采购'
          AND ifnull(cgrk.delete_flag,'0')!='1'
          AND cgrk.linked_flag = '1'
        GROUP BY cgrk.id, cgrk.number, cgrk.linked_flag
        HAVING COUNT(xsck.id) = 0
    ''')

    if not should_be_1 and not should_be_0:
        ok('linked_flag 全部一致')
        return

    if should_be_1:
        warn(f'应为1但当前为0: {len(should_be_1)} 条')
        print_table(should_be_1, ['number', 'linked_flag'])
    if should_be_0:
        warn(f'应为0但当前为1: {len(should_be_0)} 条')
        print_table(should_be_0, ['number', 'linked_flag'])

    if not confirm():
        return

    for r in should_be_1:
        execute('UPDATE jsh_depot_head SET linked_flag=%s WHERE id=%s', ('1', r['id']))
    for r in should_be_0:
        execute('UPDATE jsh_depot_head SET linked_flag=%s WHERE id=%s', ('0', r['id']))
    ok(f'已修复 {len(should_be_1) + len(should_be_0)} 条')

# ─── 5. 检查重复关联 ────────────────────────────────────────────
def cmd_check_dup_link():
    title('检查重复关联 (link_number 去重)')
    rows = query('''
        SELECT id, number, link_number
        FROM jsh_depot_head
        WHERE ifnull(delete_flag,'0')!='1'
          AND link_number IS NOT NULL AND link_number LIKE '%,%'
    ''')

    dups = []
    for r in rows:
        parts = [p.strip() for p in r['link_number'].split(',') if p.strip()]
        unique = list(dict.fromkeys(parts))  # 保持顺序去重
        if len(unique) < len(parts):
            dups.append({
                'id': r['id'],
                'number': r['number'],
                'current': r['link_number'],
                'fixed': ','.join(unique)
            })

    if not dups:
        ok('无重复关联')
        return

    print_table(dups, ['number', 'current', 'fixed'])
    if not confirm():
        return

    for d in dups:
        execute('UPDATE jsh_depot_head SET link_number=%s WHERE id=%s', (d['fixed'], d['id']))
    ok(f'已修复 {len(dups)} 条')

# ─── 6. 检查销售出库关联一致性 ──────────────────────────────────
def cmd_check_xsck_consistency():
    title('检查销售出库关联一致性')

    # link_id 指向不存在的明细
    orphans = query('''
        SELECT xsck.number, xi.id as item_id, xi.link_id
        FROM jsh_depot_head xsck
        JOIN jsh_depot_item xi ON xi.header_id = xsck.id AND ifnull(xi.delete_flag,'0')!='1'
        LEFT JOIN jsh_depot_item ci ON ci.id = xi.link_id AND ifnull(ci.delete_flag,'0')!='1'
        WHERE xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
          AND xi.link_id IS NOT NULL AND ci.id IS NULL
    ''')
    if orphans:
        err(f'link_id 指向不存在的明细: {len(orphans)} 条')
        print_table(orphans)
    else:
        ok('link_id 引用完整性正常')

    # link_number 与明细不一致
    mismatches = query('''
        SELECT xsck.number, xsck.link_number as head_link,
               GROUP_CONCAT(DISTINCT ch.number ORDER BY ch.number) as actual_link
        FROM jsh_depot_head xsck
        JOIN jsh_depot_item xi ON xi.header_id = xsck.id AND ifnull(xi.delete_flag,'0')!='1'
        JOIN jsh_depot_item ci ON ci.id = xi.link_id AND ifnull(ci.delete_flag,'0')!='1'
        JOIN jsh_depot_head ch ON ch.id = ci.header_id
        WHERE xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
          AND xi.link_id IS NOT NULL
        GROUP BY xsck.id, xsck.number, xsck.link_number
        HAVING head_link != actual_link
    ''')

    # 过滤仅顺序不同的
    real_mismatches = []
    for m in mismatches:
        head_set = set(m['head_link'].split(',')) if m['head_link'] else set()
        actual_set = set(m['actual_link'].split(',')) if m['actual_link'] else set()
        if head_set != actual_set:
            real_mismatches.append(m)

    if real_mismatches:
        err(f'link_number 与明细不一致: {len(real_mismatches)} 条')
        print_table(real_mismatches, ['number', 'head_link', 'actual_link'])
        if confirm('修复为明细实际值？(y/n): '):
            for m in real_mismatches:
                execute('UPDATE jsh_depot_head SET link_number=%s WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'',
                        (m['actual_link'], m['number']))
            ok(f'已修复 {len(real_mismatches)} 条')
    else:
        ok('link_number 与明细一致')

    # 出库数量超入库
    over = query('''
        SELECT ci.id as cgrk_item_id, ch.number as cgrk_number,
               ci.oper_number as cgrk_qty, SUM(xi.oper_number) as total_xsck_qty
        FROM jsh_depot_item xi
        JOIN jsh_depot_head xsck ON xsck.id = xi.header_id
          AND xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
        JOIN jsh_depot_item ci ON ci.id = xi.link_id AND ifnull(ci.delete_flag,'0')!='1'
        JOIN jsh_depot_head ch ON ch.id = ci.header_id
        WHERE ifnull(xi.delete_flag,'0')!='1' AND xi.link_id IS NOT NULL
        GROUP BY ci.id, ch.number, ci.oper_number
        HAVING SUM(xi.oper_number) > ci.oper_number
    ''')
    if over:
        err(f'出库数量超过入库: {len(over)} 条')
        print_table(over)
    else:
        ok('出库数量未超入库')

    # 商品不匹配
    mat_mismatch = query('''
        SELECT xsck.number, xi.material_id as xsck_mat, ci.material_id as cgrk_mat
        FROM jsh_depot_head xsck
        JOIN jsh_depot_item xi ON xi.header_id = xsck.id AND ifnull(xi.delete_flag,'0')!='1'
        JOIN jsh_depot_item ci ON ci.id = xi.link_id AND ifnull(ci.delete_flag,'0')!='1'
        WHERE xsck.type='出库' AND xsck.sub_type='销售' AND ifnull(xsck.delete_flag,'0')!='1'
          AND xi.link_id IS NOT NULL AND xi.material_id != ci.material_id
    ''')
    if mat_mismatch:
        err(f'商品不匹配: {len(mat_mismatch)} 条')
        print_table(mat_mismatch)
    else:
        ok('商品匹配正常')

# ─── 7. 查询反向关联链路 ────────────────────────────────────────
def cmd_trace():
    title('查询反向关联链路')
    numbers = input_numbers('请输入单据号: ')
    if not numbers:
        return

    for num in numbers:
        print(f'\n  {C.BOLD}── {num} ──{C.RESET}')

        # 先查 depot_head
        head = query('SELECT id, number, type, sub_type, link_number, organ_id FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (num,))
        if head:
            h = head[0]
            print(f'  类型: {h["type"]}/{h["sub_type"]}  organ_id: {h["organ_id"]}')

            # 上游 (link_number)
            if h['link_number']:
                links = h['link_number'].split(',')
                for lk in links:
                    lk = lk.strip()
                    up = query('SELECT number, type, sub_type, link_number FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (lk,))
                    if up:
                        u = up[0]
                        arrow = f'{num} → {u["number"]}({u["type"]}/{u["sub_type"]})'
                        if u['link_number']:
                            arrow += f' → {u["link_number"]}'
                        print(f'  {C.GREEN}↑ 上游: {arrow}{C.RESET}')

            # 下游 (被引用)
            downs = query('SELECT number, type, sub_type FROM jsh_depot_head WHERE FIND_IN_SET(%s, link_number)>0 AND ifnull(delete_flag,\'0\')!=\'1\'', (num,))
            for d in downs:
                print(f'  {C.CYAN}↓ 下游: {d["number"]}({d["type"]}/{d["sub_type"]}){C.RESET}')

            # 物流单
            freight = query('''
                SELECT fh.bill_no FROM jsh_freight_head fh
                JOIN jsh_freight_item fi ON fi.header_id = fh.id AND ifnull(fi.delete_flag,'0')!='1'
                WHERE fi.depot_head_id = %s AND ifnull(fh.delete_flag,'0')!='1'
            ''', (h['id'],))
            for f in freight:
                print(f'  {C.YELLOW}📦 物流单: {f["bill_no"]}{C.RESET}')
            continue

        # 查物流单
        fh = query('SELECT id, bill_no FROM jsh_freight_head WHERE bill_no=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (num,))
        if fh:
            items = query('''
                SELECT fi.depot_head_id, dh.number as xsck, dh.link_number
                FROM jsh_freight_item fi
                LEFT JOIN jsh_depot_head dh ON dh.id = fi.depot_head_id AND ifnull(dh.delete_flag,'0')!='1'
                WHERE fi.header_id = %s AND ifnull(fi.delete_flag,'0')!='1'
            ''', (fh[0]['id'],))
            for it in items:
                xsck = it.get('xsck') or '(无)'
                link = it.get('link_number') or '(无)'
                print(f'  {num} → {xsck} → {link}')
                if it.get('link_number'):
                    for cgrk_num in it['link_number'].split(','):
                        cgrk_num = cgrk_num.strip()
                        cgrk = query('SELECT number, link_number FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (cgrk_num,))
                        if cgrk and cgrk[0]['link_number']:
                            print(f'    → {cgrk_num} → {cgrk[0]["link_number"]}')
        else:
            err(f'未找到: {num}')

# ─── 8. 关联单据 ────────────────────────────────────────────────
def cmd_link():
    title('关联单据')
    src = input('源单据号 (如 XSCK): ').strip()
    tgt = input('目标单据号 (如 CGRK): ').strip()
    if not src or not tgt:
        return

    src_head = query('SELECT id, number, type, sub_type, link_number FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (src,))
    tgt_head = query('SELECT id, number, type, sub_type FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (tgt,))
    if not src_head:
        err(f'源单据不存在: {src}')
        return
    if not tgt_head:
        err(f'目标单据不存在: {tgt}')
        return

    s, t = src_head[0], tgt_head[0]
    print(f'  源: {s["number"]} ({s["type"]}/{s["sub_type"]})')
    print(f'  目标: {t["number"]} ({t["type"]}/{t["sub_type"]})')

    # 合并 link_number
    old_link = s.get('link_number') or ''
    existing = set(n.strip() for n in old_link.split(',') if n.strip())
    if tgt in existing:
        warn(f'{src} 已关联 {tgt}')
        return
    existing.add(tgt)
    new_link = ','.join(sorted(existing))

    # 查看明细匹配
    src_items = query('''
        SELECT di.id, di.material_id, m.name, di.link_id
        FROM jsh_depot_item di LEFT JOIN jsh_material m ON m.id = di.material_id
        WHERE di.header_id = %s AND ifnull(di.delete_flag,'0')!='1'
    ''', (s['id'],))
    tgt_items = query('''
        SELECT di.id, di.material_id, m.name
        FROM jsh_depot_item di LEFT JOIN jsh_material m ON m.id = di.material_id
        WHERE di.header_id = %s AND ifnull(di.delete_flag,'0')!='1'
    ''', (t['id'],))

    print(f'\n  {C.BOLD}源明细:{C.RESET}')
    print_table(src_items, ['id', 'material_id', 'name', 'link_id'])
    print(f'\n  {C.BOLD}目标明细:{C.RESET}')
    print_table(tgt_items, ['id', 'material_id', 'name'])

    print(f'\n  link_number: {old_link or "(空)"} → {new_link}')

    # 自动匹配明细 link_id (按 material_id)
    link_plan = []
    tgt_map = {}
    for ti in tgt_items:
        tgt_map.setdefault(ti['material_id'], []).append(ti['id'])
    for si in src_items:
        if si['link_id']:
            continue
        candidates = tgt_map.get(si['material_id'], [])
        if candidates:
            link_plan.append((si['id'], candidates[0]))
            candidates.pop(0)

    if link_plan:
        print(f'\n  {C.BOLD}明细 link_id 匹配:{C.RESET}')
        for src_id, tgt_id in link_plan:
            print(f'    源明细 {src_id} → 目标明细 {tgt_id}')

    if not confirm():
        return

    execute('UPDATE jsh_depot_head SET link_number=%s WHERE id=%s', (new_link, s['id']))
    for src_id, tgt_id in link_plan:
        execute('UPDATE jsh_depot_item SET link_id=%s WHERE id=%s', (tgt_id, src_id))
    execute('UPDATE jsh_depot_head SET linked_flag=%s WHERE id=%s', ('1', t['id']))
    ok('关联完成')

# ─── 9. 删除单据 ────────────────────────────────────────────────
def cmd_delete():
    title('删除单据 (逻辑删除)')
    numbers = input_numbers()
    if not numbers:
        return

    for num in numbers:
        head = query('SELECT id, number, type, sub_type, status, link_number FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (num,))
        if not head:
            err(f'未找到: {num}')
            continue
        h = head[0]
        print(f'\n  {h["number"]}  {h["type"]}/{h["sub_type"]}  状态:{h["status"]}  关联:{h.get("link_number") or "(无)"}')

        warn(f'即将逻辑删除 {num} 及其明细')
        if not confirm(f'确认删除 {num}？(y/n): '):
            continue

        execute('UPDATE jsh_depot_head SET delete_flag=%s WHERE id=%s', ('1', h['id']))
        execute('UPDATE jsh_depot_item SET delete_flag=%s WHERE header_id=%s', ('1', h['id']))

        # 恢复被关联单据的 linked_flag
        if h.get('link_number'):
            for lk in h['link_number'].split(','):
                lk = lk.strip()
                if not lk:
                    continue
                # 检查是否还有其他单据引用该 lk
                refs = query('''
                    SELECT COUNT(*) as cnt FROM jsh_depot_head
                    WHERE FIND_IN_SET(%s, link_number) > 0
                      AND ifnull(delete_flag,'0')!='1' AND number != %s
                ''', (lk, num))
                if refs and refs[0]['cnt'] == 0:
                    execute('UPDATE jsh_depot_head SET linked_flag=%s WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', ('0', lk))
                    info(f'  已恢复 {lk} 的 linked_flag=0')

        ok(f'已删除 {num}')

# ─── 10. 修改单据状态 ───────────────────────────────────────────
def cmd_status():
    title('修改单据状态')
    num = input('单据号: ').strip()
    if not num:
        return
    head = query('SELECT id, number, status, type, sub_type FROM jsh_depot_head WHERE number=%s AND ifnull(delete_flag,\'0\')!=\'1\'', (num,))
    if not head:
        err(f'未找到: {num}')
        return
    h = head[0]
    print(f'  {h["number"]}  {h["type"]}/{h["sub_type"]}  当前状态: {h["status"]}')
    print(f'  状态说明: 0=未审核  1=已审核  2=完成  3=部分')

    new_status = input('目标状态: ').strip()
    if new_status not in ('0', '1', '2', '3', '9'):
        err('无效状态')
        return
    if not confirm(f'将 {num} 状态从 {h["status"]} 改为 {new_status}？(y/n): '):
        return

    execute('UPDATE jsh_depot_head SET status=%s WHERE id=%s', (new_status, h['id']))
    ok(f'已将 {num} 状态改为 {new_status}')

# ─── 主菜单 ─────────────────────────────────────────────────────
MENU = [
    ('1',  '诊断单据',                          cmd_diagnose),
    ('2',  '修复优惠后金额 (discount_last_money)', cmd_fix_discount_last_money),
    ('3',  '补全采购订单关联出库 (CGDD→XSCK)',     cmd_fill_cgdd_link),
    ('4',  '同步 linked_flag 标记',              cmd_sync_linked_flag),
    ('5',  '检查重复关联 (link_number 去重)',      cmd_check_dup_link),
    ('6',  '检查销售出库关联一致性',               cmd_check_xsck_consistency),
    ('7',  '查询反向关联链路',                     cmd_trace),
    ('8',  '关联单据 (手动)',                      cmd_link),
    ('9',  '删除单据 (逻辑删除)',                  cmd_delete),
    ('10', '修改单据状态',                         cmd_status),
]

# ─── 命令行参数支持 ──────────────────────────────────────────────
CLI_MAP = {
    'diagnose':   cmd_diagnose,
    'fix-amount': cmd_fix_discount_last_money,
    'fill-cgdd':  cmd_fill_cgdd_link,
    'sync-flag':  cmd_sync_linked_flag,
    'check-dup':  cmd_check_dup_link,
    'check-xsck': cmd_check_xsck_consistency,
    'trace':      cmd_trace,
    'link':       cmd_link,
    'delete':     cmd_delete,
    'status':     cmd_status,
}

def interactive():
    while True:
        print(f'\n{C.BOLD}{"=" * 40}{C.RESET}')
        print(f'{C.BOLD}  jshERP 数据库运维工具{C.RESET}')
        print(f'{C.BOLD}{"=" * 40}{C.RESET}')
        for key, label, _ in MENU:
            print(f'  {C.CYAN}{key:>2}{C.RESET}. {label}')
        print(f'  {C.CYAN} 0{C.RESET}. 退出')

        choice = input(f'\n{C.BOLD}请选择: {C.RESET}').strip()
        if choice == '0' or choice.lower() in ('q', 'quit', 'exit'):
            print('再见')
            break

        found = False
        for key, _, func in MENU:
            if choice == key:
                try:
                    func()
                except KeyboardInterrupt:
                    print('\n  已取消')
                except Exception as e:
                    err(f'执行异常: {e}')
                found = True
                break
        if not found:
            err('无效选项')

def main():
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
        if cmd in CLI_MAP:
            try:
                CLI_MAP[cmd]()
            except KeyboardInterrupt:
                print('\n已取消')
        elif cmd == '--help':
            print('用法: erp_ops.py [command]')
            print('可用命令:', ', '.join(CLI_MAP.keys()))
        else:
            err(f'未知命令: {cmd}')
            print('可用命令:', ', '.join(CLI_MAP.keys()))
    else:
        interactive()

if __name__ == '__main__':
    main()
