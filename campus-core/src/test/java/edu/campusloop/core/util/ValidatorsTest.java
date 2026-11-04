package edu.campusloop.core.util;

import edu.campusloop.core.exception.ValidationException;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class ValidatorsTest {

    @Test
    public void acceptsValidUsername() {
        assertEquals("student_01", Validators.username("student_01"));
    }

    @Test(expected = ValidationException.class)
    public void rejectsTooShortUsername() {
        Validators.username("ab");
    }

    @Test(expected = ValidationException.class)
    public void rejectsUsernameWithIllegalCharacters() {
        Validators.username("bad name!");
    }

    @Test
    public void acceptsValidPassword() {
        assertEquals("Abc12345", Validators.password("Abc12345"));
    }

    @Test(expected = ValidationException.class)
    public void rejectsPasswordWithoutDigit() {
        Validators.password("abcdef");
    }

    @Test(expected = ValidationException.class)
    public void rejectsPasswordThatIsTooShort() {
        Validators.password("a1");
    }

    @Test
    public void trimsTitle() {
        assertEquals("台灯", Validators.title("  台灯  "));
    }

    @Test(expected = ValidationException.class)
    public void rejectsBlankDescription() {
        Validators.description("   ");
    }

    @Test(expected = ValidationException.class)
    public void rejectsNegativePrice() {
        Validators.price(new BigDecimal("-1"));
    }

    @Test
    public void acceptsZeroPrice() {
        assertEquals(0, Validators.price(BigDecimal.ZERO).signum());
    }

    @Test(expected = ValidationException.class)
    public void rejectsRatingOutOfRange() {
        Validators.rating(6);
    }

    @Test(expected = ValidationException.class)
    public void rejectsNonPositiveId() {
        Validators.requirePositiveId("物品", 0);
    }
}
