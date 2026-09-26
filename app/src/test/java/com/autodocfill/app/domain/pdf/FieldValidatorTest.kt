package com.autodocfill.app.domain.pdf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldValidatorTest {

    private fun valid(key: String, value: String) = FieldValidator.validate(key, value).isValid

    @Test
    fun `empty values are always valid`() {
        assertTrue(valid("email", ""))
        assertTrue(valid("dateOfBirth", ""))
    }

    @Test
    fun `validates email`() {
        assertTrue(valid("email", "jane.doe@example.com"))
        assertTrue(valid("workEmail", "jane+forms@example.co.za"))
        assertFalse(valid("email", "jane.doe@"))
        assertFalse(valid("email", "not an email"))
    }

    @Test
    fun `validates phone numbers with common formatting`() {
        assertTrue(valid("phoneNumber", "+27 82 123 4567"))
        assertTrue(valid("phoneNumber", "(123) 456-7890"))
        assertFalse(valid("phoneNumber", "12ab"))
    }

    @Test
    fun `accepts US ZIP and international postal codes`() {
        assertTrue(valid("zipCode", "12345"))
        assertTrue(valid("zipCode", "12345-6789"))
        assertTrue(valid("zipCode", "SW1A 1AA"))
        assertTrue(valid("zipCode", "8001"))
        assertFalse(valid("zipCode", "!!"))
    }

    @Test
    fun `validates real calendar dates`() {
        assertTrue(valid("dateOfBirth", "1990-05-17"))
        assertTrue(valid("dateOfBirth", "2000-02-29"))
        assertFalse(valid("dateOfBirth", "1999-02-29"))
        assertFalse(valid("dateOfBirth", "1990-04-31"))
        assertFalse(valid("dateOfBirth", "17/05/1990"))
    }

    @Test
    fun `validates id numbers`() {
        assertTrue(valid("idNumber", "9001015009087"))
        assertFalse(valid("idNumber", "123"))
    }

    @Test
    fun `other keys are not validated`() {
        assertTrue(valid("company", "Anything at all!"))
    }
}
