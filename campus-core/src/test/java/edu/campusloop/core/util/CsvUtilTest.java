package edu.campusloop.core.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void escapesCommaAndQuotes() {
        assertEquals("\"a,b\"", CsvUtil.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvUtil.escape("say \"hi\""));
        assertEquals("plain", CsvUtil.escape("plain"));
    }

    @Test
    public void escapesLineBreaks() {
        assertEquals("\"line1\nline2\"", CsvUtil.escape("line1\nline2"));
    }

    @Test
    public void writesUtf8BomAndRows() throws Exception {
        File file = folder.newFile("report.csv");
        Path path = file.toPath();
        List<String> headers = Arrays.asList("分类", "数量");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("教材书籍", "3"),
                Arrays.asList("数码配件", "2"));

        CsvUtil.write(path, headers, rows);

        byte[] bytes = Files.readAllBytes(path);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("分类,数量"));
        assertTrue(content.contains("教材书籍,3"));
        assertTrue(content.contains("数码配件,2"));
    }
}
