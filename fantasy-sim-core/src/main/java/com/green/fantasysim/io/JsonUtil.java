package com.green.fantasysim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonUtil {
    private static final ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private JsonUtil(){}

    public static <T> T read(InputStream in, Class<T> clazz) {
        try { return om.readValue(in, clazz); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static <T> T readFile(Path path, Class<T> clazz) {
        try (InputStream in = Files.newInputStream(path)) { return read(in, clazz); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void writeFile(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) { om.writeValue(out, value); }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper mapper() { return om; }
}
