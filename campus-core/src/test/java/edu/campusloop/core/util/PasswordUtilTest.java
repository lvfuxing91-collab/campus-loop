package edu.campusloop.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordUtilTest {

    @Test
    public void hashIsDeterministicForSameSalt() {
        String salt = "fixed-salt";
        assertEquals(PasswordUtil.hash("Secret123", salt), PasswordUtil.hash("Secret123", salt));
    }

    @Test
    public void differentSaltsProduceDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("Secret123", "salt-a"), PasswordUtil.hash("Secret123", "salt-b"));
    }

    @Test
    public void verifyAcceptsCorrectPassword() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash("Secret123", salt);
        assertTrue(PasswordUtil.verify("Secret123", salt, hash));
    }

    @Test
    public void verifyRejectsWrongPassword() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash("Secret123", salt);
        assertFalse(PasswordUtil.verify("Secret124", salt, hash));
    }

    @Test
    public void verifyRejectsNullInput() {
        assertFalse(PasswordUtil.verify(null, "salt", "hash"));
        assertFalse(PasswordUtil.verify("pwd", null, "hash"));
        assertFalse(PasswordUtil.verify("pwd", "salt", null));
    }

    @Test
    public void generatedSaltIsHexAndUnique() {
        String first = PasswordUtil.generateSalt();
        String second = PasswordUtil.generateSalt();
        assertEquals(32, first.length());
        assertTrue(first.matches("[0-9a-f]+"));
        assertNotEquals(first, second);
    }
}
