package org.lts.demotivator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LanguageFilesTest {
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

    @Test
    void allLocalesHaveTheSameKeys() throws IOException {
        Path dir = Path.of("src/client/resources/assets/demotivator/lang");
        Set<String> expected = read(dir.resolve("en_us.json")).keySet();
        for (String locale : new String[]{"ru_ru.json", "es_es.json", "es_mx.json"}) {
            assertEquals(expected, read(dir.resolve(locale)).keySet(), locale);
        }
    }

    private static Map<String, String> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, MAP_TYPE);
        }
    }
}
