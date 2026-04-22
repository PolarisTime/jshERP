#!/usr/bin/env python3
import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter


LOGIN_PATH = "/user/login"


def request_json(method, url, headers=None, body=None, timeout=15):
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method)
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content = resp.read().decode("utf-8", errors="replace")
            return resp.status, content
    except urllib.error.HTTPError as exc:
        content = exc.read().decode("utf-8", errors="replace")
        return exc.code, content


def default_value(name, schema, for_path=False):
    lname = name.lower()
    type_name = (schema or {}).get("type", "")
    if lname in {"currentpage", "page", "pageindex"}:
        return "1"
    if lname in {"pagesize", "size", "limit"}:
        return "10"
    if "begintime" in lname or lname == "begindate":
        return "2026-04-01"
    if "endtime" in lname or lname == "enddate":
        return "2026-04-30"
    if lname in {"status", "signstatus", "deliverystatus", "paymentstatus", "auditstatus"}:
        return "0"
    if lname in {"deliverydate"}:
        return "2026-04-22"
    if lname in {"id", "userid", "organid", "accountid", "depotid", "carrierid", "categoryid",
                 "organizationid", "approvalid", "statementid", "supplierid", "roleid"}:
        return "1"
    if lname in {"pnumber"}:
        return "0"
    if lname in {"number", "billno", "contractno"}:
        return "1"
    if type_name in {"integer", "number"}:
        return "1"
    if type_name == "boolean":
        return "false"
    if for_path:
        return "1"
    return ""


def build_request(base_url, path, method, operation, user_id, token):
    headers = {"X-Access-Token": token}
    params = []
    body = None
    content_type = None

    if path == LOGIN_PATH and method == "post":
        headers = {"Content-Type": "application/json"}
        body = {"loginName": "sakura", "password": "", "code": "", "uuid": ""}
        return base_url + path, headers, body, False

    if path == "/function/findMenuByPNumber" and method == "post":
        headers["Content-Type"] = "application/json"
        body = {"pNumber": "0", "userId": str(user_id)}
        return base_url + path, headers, body, False

    request_body = operation.get("requestBody", {})
    content = request_body.get("content", {})
    if "multipart/form-data" in content:
        return None, None, None, True
    if "application/json" in content:
        headers["Content-Type"] = "application/json"
        body = {}
    elif "application/x-www-form-urlencoded" in content:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        body = {}
    elif content:
        content_type = next(iter(content.keys()))
        if "json" in content_type:
            headers["Content-Type"] = content_type
            body = {}

    real_path = path
    for param in operation.get("parameters", []):
        name = param.get("name")
        location = param.get("in")
        schema = param.get("schema", {})
        value = default_value(name, schema, for_path=location == "path")
        if location == "path":
            real_path = real_path.replace("{" + name + "}", urllib.parse.quote(str(value)))
        elif location == "query":
            params.append((name, value))

    if params:
        real_path = real_path + "?" + urllib.parse.urlencode(params)
    return base_url + real_path, headers, body, False


def normalize_base(base_url):
    return base_url.rstrip("/")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--login-name", default="sakura")
    parser.add_argument("--password-hash", required=True)
    parser.add_argument("--timeout", type=int, default=15)
    args = parser.parse_args()

    base_url = normalize_base(args.base_url)
    login_url = base_url + LOGIN_PATH
    status, content = request_json(
        "POST",
        login_url,
        headers={"Content-Type": "application/json"},
        body={
            "loginName": args.login_name,
            "password": args.password_hash,
            "code": "",
            "uuid": "",
        },
        timeout=args.timeout,
    )
    if status != 200:
        print(json.dumps({"login_status": status, "login_body": content}, ensure_ascii=False, indent=2))
        sys.exit(1)

    login_data = json.loads(content)
    token = login_data["data"]["token"]
    user_id = login_data["data"]["user"]["id"]

    api_status, api_content = request_json("GET", base_url + "/v3/api-docs", timeout=args.timeout)
    if api_status != 200:
        print(json.dumps({"api_status": api_status, "api_body": api_content}, ensure_ascii=False, indent=2))
        sys.exit(1)
    spec = json.loads(api_content)

    results = []
    counter = Counter()
    start = time.time()
    for path, methods in spec.get("paths", {}).items():
        for method, operation in methods.items():
            method_upper = method.upper()
            url, headers, body, skipped = build_request(base_url, path, method, operation, user_id, token)
            item = {
                "method": method_upper,
                "path": path,
                "operationId": operation.get("operationId"),
            }
            if skipped:
                item["status"] = "skipped_multipart"
                counter["skipped_multipart"] += 1
                results.append(item)
                continue
            req_body = body
            if path == LOGIN_PATH and method == "post":
                req_body["password"] = args.password_hash
            try:
                status, body_text = request_json(method_upper, url, headers=headers, body=req_body, timeout=args.timeout)
                item["httpStatus"] = status
                item["bodyPreview"] = body_text[:200]
                group = f"{status // 100}xx"
                counter[group] += 1
            except Exception as exc:  # pragma: no cover
                item["status"] = "exception"
                item["error"] = str(exc)
                counter["exception"] += 1
            results.append(item)

    summary = {
        "baseUrl": base_url,
        "loginName": args.login_name,
        "totalOperations": len(results),
        "elapsedSeconds": round(time.time() - start, 2),
        "counts": counter,
        "failures": [item for item in results if item.get("httpStatus", 0) >= 500 or item.get("status") == "exception"],
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2, default=lambda obj: dict(obj)))


if __name__ == "__main__":
    main()
