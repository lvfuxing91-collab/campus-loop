package edu.campusloop.core.util;

import edu.campusloop.core.exception.AppException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvUtil {

    private static final char BOM = '﻿';

    private CsvUtil() {
    }

    public static void write(Path file, List<String> headers, List<List<String>> rows) {
        if (file == null) {
            throw new IllegalArgumentException("导出路径不能为空");
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(BOM);
                if (headers != null && !headers.isEmpty()) {
                    writer.write(join(headers));
                    writer.write("\r\n");
                }
                if (rows != null) {
                    for (List<String> row : rows) {
                        writer.write(join(row));
                        writer.write("\r\n");
                    }
                }
            }
        } catch (IOException e) {
            throw new AppException("导出 CSV 失败：" + file, e);
        }
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String join(List<String> cells) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escape(cells.get(i)));
        }
        return builder.toString();
    }
}
