package com.newpohone;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProyectoGa722501096ApplicationTests {

    @BeforeAll
    static void prepareDatabase() throws Exception {
        Path db = Path.of(System.getProperty("java.io.tmpdir"), "newphone-test-" + System.nanoTime() + ".db");
        Files.deleteIfExists(db);
        System.setProperty("newphone.database", db.toAbsolutePath().toString());
    }

    @Test
    void contextLoads() {
    }
}
