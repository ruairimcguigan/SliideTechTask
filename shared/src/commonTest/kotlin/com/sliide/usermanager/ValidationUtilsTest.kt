package com.sliide.usermanager

import com.sliide.usermanager.domain.usecase.ValidationResult
import com.sliide.usermanager.domain.usecase.ValidationUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidationUtilsTest {

    // ─── Name Validation ─────────────────────────────────────

    @Test
    fun validName_returnsValid() {
        assertTrue(ValidationUtils.validateName("John Smith").isValid)
    }

    @Test
    fun validName_withHyphen() {
        assertTrue(ValidationUtils.validateName("Mary-Jane Watson").isValid)
    }

    @Test
    fun validName_withApostrophe() {
        assertTrue(ValidationUtils.validateName("O'Brien").isValid)
    }

    @Test
    fun validName_singleWord() {
        assertTrue(ValidationUtils.validateName("Madonna").isValid)
    }

    @Test
    fun invalidName_blank() {
        val result = ValidationUtils.validateName("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Name is required", result.message)
    }

    @Test
    fun invalidName_whitespaceOnly() {
        val result = ValidationUtils.validateName("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Name is required", result.message)
    }

    @Test
    fun invalidName_tooShort() {
        val result = ValidationUtils.validateName("A")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Name must be at least 2 characters", result.message)
    }

    @Test
    fun invalidName_tooLong() {
        val longName = "A".repeat(101)
        val result = ValidationUtils.validateName(longName)
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Name must be under 100 characters", result.message)
    }

    @Test
    fun invalidName_containsNumbers() {
        val result = ValidationUtils.validateName("John123")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun invalidName_containsSpecialChars() {
        val result = ValidationUtils.validateName("John@Smith")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun validName_trimmed() {
        assertTrue(ValidationUtils.validateName("  John Smith  ").isValid)
    }

    // ─── Email Validation ────────────────────────────────────

    @Test
    fun validEmail_standard() {
        assertTrue(ValidationUtils.validateEmail("john@example.com").isValid)
    }

    @Test
    fun validEmail_withPlus() {
        assertTrue(ValidationUtils.validateEmail("john+tag@example.com").isValid)
    }

    @Test
    fun validEmail_withDots() {
        assertTrue(ValidationUtils.validateEmail("john.smith@example.co.uk").isValid)
    }

    @Test
    fun validEmail_withNumbers() {
        assertTrue(ValidationUtils.validateEmail("user123@test456.org").isValid)
    }

    @Test
    fun invalidEmail_blank() {
        val result = ValidationUtils.validateEmail("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Email is required", result.message)
    }

    @Test
    fun invalidEmail_noAtSign() {
        val result = ValidationUtils.validateEmail("john.example.com")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Please enter a valid email address", result.message)
    }

    @Test
    fun invalidEmail_noDomain() {
        val result = ValidationUtils.validateEmail("john@")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun invalidEmail_noTLD() {
        val result = ValidationUtils.validateEmail("john@example")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun invalidEmail_doubleAt() {
        val result = ValidationUtils.validateEmail("john@@example.com")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun invalidEmail_spaces() {
        val result = ValidationUtils.validateEmail("john smith@example.com")
        assertIs<ValidationResult.Invalid>(result)
    }

    @Test
    fun invalidEmail_tooLong() {
        val longEmail = "a".repeat(250) + "@b.com"
        val result = ValidationUtils.validateEmail(longEmail)
        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Email is too long", result.message)
    }

    @Test
    fun validEmail_trimmed() {
        assertTrue(ValidationUtils.validateEmail("  john@example.com  ").isValid)
    }
}
