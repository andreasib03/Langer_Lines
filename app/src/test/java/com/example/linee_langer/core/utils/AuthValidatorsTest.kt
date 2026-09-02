package com.example.linee_langer.core.utils

import org.junit.Assert.*
import org.junit.Test

class AuthValidatorsTest {

    @Test
    fun `isEmailValid returns true for correct emails`() {
        assertTrue(AuthValidators.isEmailValid("test@example.com"))
        assertTrue(AuthValidators.isEmailValid("user.name@domain.it"))
        assertTrue(AuthValidators.isEmailValid("info123@sub.domain.org"))
    }

    @Test
    fun `isEmailValid returns false for malformed emails`() {
        assertFalse(AuthValidators.isEmailValid("plainaddress"))
        assertFalse(AuthValidators.isEmailValid("@missinguser.com"))
        assertFalse(AuthValidators.isEmailValid("user@domain..com"))
        assertFalse(AuthValidators.isEmailValid("user@domain"))
        assertFalse(AuthValidators.isEmailValid(""))
    }

    @Test
    fun `isPasswordValid returns true for strong passwords`() {
        // Almeno 8 caratteri, una maiuscola, un numero, un carattere speciale
        assertTrue(AuthValidators.isPasswordValid("Password123!"))
        assertTrue(AuthValidators.isPasswordValid("S3cur3P@ss!"))
    }

    @Test
    fun `isPasswordValid returns false for weak passwords`() {
        assertFalse(AuthValidators.isPasswordValid("short1!")) // Troppo corta
        assertFalse(AuthValidators.isPasswordValid("lowercase123!")) // No maiuscola
        assertFalse(AuthValidators.isPasswordValid("UPPERCASE123!")) // No minuscola (anche se non esplicitamente richiesta, spesso inclusa)
        assertFalse(AuthValidators.isPasswordValid("NoSpecialChar123")) // No speciale
        assertFalse(AuthValidators.isPasswordValid("NoNumber!!!")) // No numero
    }

    @Test
    fun `validateBirthDate identifies underage users`() {
        // Supponendo oggi sia 2026, uno nato nel 2020 è troppo giovane
        val result = AuthValidators.validateBirthDate("01/01/2020")
        assertTrue(result is AgeValidationResult.TooYoung)
    }

    @Test
    fun `validateBirthDate identifies valid dates`() {
        // Uno nato nel 1990 ha circa 36 anni (nel 2026)
        val result = AuthValidators.validateBirthDate("15/05/1990")
        assertTrue(result is AgeValidationResult.Valid)
        assertEquals(36, (result as AgeValidationResult.Valid).age)
    }

    @Test
    fun `validateBirthDate catches invalid formats`() {
        val result = AuthValidators.validateBirthDate("2020-01-01")
        assertTrue(result is AgeValidationResult.InvalidFormat)
    }
}
