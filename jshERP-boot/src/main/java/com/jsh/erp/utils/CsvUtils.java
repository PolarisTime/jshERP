package com.jsh.erp.utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV导入导出工具类
 */
public class CsvUtils {

    private static final String BOM = "\uFEFF";

    /**
     * 将值转为CSV安全格式（双引号包裹，内部双引号转义）
     */
    private static String escapeCsv(Object val) {
        String s = val != null ? val.toString() : "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /**
     * 导出CSV文件到临时目录
     */
    public static File exportCsv(String path, String fileName, String tip,
                                  String[] names, List<Object[]> objects) throws Exception {
        boolean dirOk = FileUtils.makedir(path);
        if (!dirOk || !new File(path).canWrite()) {
            path = System.getProperty("java.io.tmpdir");
        }
        File csvFile = new File(path + File.separator + fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8")) {
            writer.write(BOM);
            // 写入表头
            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                if (i > 0) headerLine.append(",");
                headerLine.append(escapeCsv(names[i]));
            }
            writer.write(headerLine.toString());
            writer.write("\n");
            // 写入数据行
            if (objects != null) {
                for (Object[] row : objects) {
                    StringBuilder dataLine = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        if (i > 0) dataLine.append(",");
                        dataLine.append(escapeCsv(row[i]));
                    }
                    writer.write(dataLine.toString());
                    writer.write("\n");
                }
            }
        }
        return csvFile;
    }

    /**
     * 导出CSV（多sheet兼容，忽略sheet概念，直接追加写入数据）
     */
    public static void exportCsvManySheet(OutputStreamWriter writer, String tip,
                                           String[] names, List<String[]> objects) throws Exception {
        // 写入表头
        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) headerLine.append(",");
            headerLine.append(escapeCsv(names[i].replace("*", "")));
        }
        writer.write(headerLine.toString());
        writer.write("\n");
        // 写入数据行
        if (objects != null) {
            for (String[] row : objects) {
                StringBuilder dataLine = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) dataLine.append(",");
                    dataLine.append(escapeCsv(row[i]));
                }
                writer.write(dataLine.toString());
                writer.write("\n");
            }
        }
    }

    /**
     * 下载CSV文件
     */
    public static void downloadCsv(File csvFile, String fileName, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv;charset=utf-8");
        fileName = new String(fileName.getBytes("gbk"), "ISO8859_1");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + ".csv\"");
        FileInputStream fis = new FileInputStream(csvFile);
        OutputStream out = response.getOutputStream();
        int SIZE = 1024 * 1024;
        byte[] bytes = new byte[SIZE];
        int LENGTH;
        while ((LENGTH = fis.read(bytes)) != -1) {
            out.write(bytes, 0, LENGTH);
        }
        out.flush();
        fis.close();
    }

    // ======================== CSV解析（导入） ========================

    /**
     * 检测字节数组的编码：优先识别 UTF-8 BOM，其次校验 UTF-8 合法性，不合法则回退 GBK。
     * 适用于 Windows 用户用 Excel 编辑 CSV 后保存为 ANSI(GBK) 编码的场景。
     */
    private static String detectCharset(byte[] data) {
        // 检测 UTF-8 BOM
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return "UTF-8";
        }
        // 校验是否为合法 UTF-8
        if (isValidUtf8(data)) {
            return "UTF-8";
        }
        return "GBK";
    }

    /**
     * 判断字节数组是否为合法的 UTF-8 编码（含非 ASCII 字符时才有意义）
     */
    private static boolean isValidUtf8(byte[] data) {
        boolean hasNonAscii = false;
        int i = 0;
        while (i < data.length) {
            int b = data[i] & 0xFF;
            if (b <= 0x7F) {
                i++;
            } else if (b >= 0xC2 && b <= 0xDF) {
                if (i + 1 >= data.length || (data[i + 1] & 0xC0) != 0x80) return false;
                hasNonAscii = true;
                i += 2;
            } else if (b >= 0xE0 && b <= 0xEF) {
                if (i + 2 >= data.length || (data[i + 1] & 0xC0) != 0x80 || (data[i + 2] & 0xC0) != 0x80) return false;
                hasNonAscii = true;
                i += 3;
            } else if (b >= 0xF0 && b <= 0xF4) {
                if (i + 3 >= data.length || (data[i + 1] & 0xC0) != 0x80 || (data[i + 2] & 0xC0) != 0x80 || (data[i + 3] & 0xC0) != 0x80) return false;
                hasNonAscii = true;
                i += 4;
            } else {
                return false;
            }
        }
        // 纯 ASCII 内容视为 UTF-8
        return true;
    }

    /**
     * 解析CSV文件，返回所有数据行（不含表头）
     * 第一行视为表头，从第二行起为数据行
     *
     * @param inputStream CSV文件输入流
     * @return 数据行列表，每行为String数组
     */
    public static List<String[]> parseCsv(InputStream inputStream) throws Exception {
        return parseCsv(inputStream, 1);
    }

    /**
     * 解析CSV文件，跳过指定行数后返回数据。
     * 自动检测文件编码（UTF-8 / GBK），兼容 Windows Excel 导出的 ANSI 格式。
     *
     * @param inputStream CSV文件输入流
     * @param skipRows    跳过的行数（1=跳过表头行）
     * @return 数据行列表
     */
    public static List<String[]> parseCsv(InputStream inputStream, int skipRows) throws Exception {
        // 先读取全部字节，用于编码检测
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = inputStream.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        byte[] fileBytes = baos.toByteArray();
        String charset = detectCharset(fileBytes);

        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {
            String line;
            int rowIndex = 0;
            StringBuilder currentLine = new StringBuilder();
            boolean inQuotes = false;

            while ((line = reader.readLine()) != null) {
                // 跳过BOM
                if (rowIndex == 0 && line.startsWith(BOM)) {
                    line = line.substring(1);
                }
                if (inQuotes) {
                    currentLine.append("\n").append(line);
                } else {
                    currentLine = new StringBuilder(line);
                }
                // 检查引号是否闭合
                inQuotes = !isQuotesClosed(currentLine.toString());
                if (inQuotes) {
                    continue;
                }
                if (rowIndex >= skipRows) {
                    String[] fields = parseCsvLine(currentLine.toString());
                    // 跳过全空行
                    boolean allEmpty = true;
                    for (String f : fields) {
                        if (f != null && !f.trim().isEmpty()) {
                            allEmpty = false;
                            break;
                        }
                    }
                    if (!allEmpty) {
                        result.add(fields);
                    }
                }
                rowIndex++;
            }
        }
        return result;
    }

    /**
     * 获取CSV表头行（自动检测编码）
     */
    public static String[] parseCsvHeader(InputStream inputStream) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = inputStream.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        byte[] fileBytes = baos.toByteArray();
        String charset = detectCharset(fileBytes);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {
            String line = reader.readLine();
            if (line != null) {
                if (line.startsWith(BOM)) {
                    line = line.substring(1);
                }
                return parseCsvLine(line);
            }
        }
        return new String[0];
    }

    /**
     * 获取指定行指定列的值
     */
    public static String getContent(List<String[]> rows, int rowIndex, int colIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return null;
        String[] row = rows.get(rowIndex);
        if (colIndex < 0 || colIndex >= row.length) return null;
        String val = row[colIndex];
        return val != null ? val.trim() : null;
    }

    /**
     * 获取有效数据行数（非空行）
     */
    public static int getRowCount(List<String[]> rows) {
        return rows.size();
    }

    /**
     * 解析一行CSV，处理引号转义
     */
    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(field.toString());
                    field = new StringBuilder();
                } else {
                    field.append(c);
                }
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * 检查一行中的引号是否闭合
     */
    private static boolean isQuotesClosed(String line) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++; // 跳过转义引号
                } else {
                    inQuotes = !inQuotes;
                }
            }
        }
        return !inQuotes;
    }
}
