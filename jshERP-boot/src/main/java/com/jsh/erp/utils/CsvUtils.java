package com.jsh.erp.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
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
     * 解析CSV文件，跳过指定行数后返回数据
     *
     * @param inputStream CSV文件输入流
     * @param skipRows    跳过的行数（1=跳过表头行）
     * @return 数据行列表
     */
    public static List<String[]> parseCsv(InputStream inputStream, int skipRows) throws Exception {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
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
     * 获取CSV表头行
     */
    public static String[] parseCsvHeader(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
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
