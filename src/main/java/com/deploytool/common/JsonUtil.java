package com.deploytool.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Jackson 封装：全局共享 ObjectMapper，读写 JSON 配置文件。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static <T> T read(Path file, Class<T> type) throws IOException {
        return MAPPER.readValue(file.toFile(), type);
    }

    public static void write(Path file, Object value) throws IOException {
        MAPPER.writeValue(file.toFile(), value);
    }
}
