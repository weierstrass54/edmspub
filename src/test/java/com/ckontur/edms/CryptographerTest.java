package com.ckontur.edms;

import com.ckontur.edms.component.SpringBootPostgreSQLContainerTests;
import com.ckontur.edms.component.crypto.Cryptographer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CryptographerTest extends SpringBootPostgreSQLContainerTests {
    @Autowired
    private Cryptographer cryptographer;

    @Test
    public void messageEncryptDecryptTest() {
        String message = "Awaken, my masters!";
        String encrypted = cryptographer.encrypt(message).get();
        String decrypted = cryptographer.decrypt(encrypted).get();
        assertEquals(decrypted, message);
    }

}
