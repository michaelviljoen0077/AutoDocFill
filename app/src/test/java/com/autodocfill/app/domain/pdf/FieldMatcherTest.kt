package com.autodocfill.app.domain.pdf

import com.autodocfill.app.data.model.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldMatcherTest {

    @Test
    fun `maps common field name styles to profile keys`() {
        mapOf(
            "first_name" to "firstName",
            "txtFirstName" to "firstName",
            "Given Name" to "firstName",
            "Surname" to "lastName",
            "applicant_last_name" to "lastName",
            "Middle Name" to "middleName",
            "Full Name" to "fullName",
            "Name" to "fullName",
            "E-mail Address" to "email",
            "email" to "email",
            "Phone" to "phoneNumber",
            "Mobile Number" to "phoneNumber",
            "Address Line 1" to "addressLine1",
            "Street Address" to "addressLine1",
            "Address Line 2" to "addressLine2",
            "City" to "city",
            "State/Province" to "state",
            "Zip Code" to "zipCode",
            "Postal Code" to "zipCode",
            "Country" to "country",
            "Date of Birth" to "dateOfBirth",
            "DOB" to "dateOfBirth",
            "SSN" to "idNumber",
            "Passport No" to "passportNumber",
            "Driver License" to "driverLicenseNumber",
            "Tax ID" to "taxNumber",
            "Gender" to "gender",
            "Occupation" to "occupation",
            "Applicant Signature" to FieldMatcher.SIGNATURE_KEY
        ).forEach { (fieldName, expected) ->
            assertEquals("for '$fieldName'", expected, FieldMatcher.mapToProfileKey(fieldName))
        }
    }

    @Test
    fun `specific names win over the generic name rule`() {
        assertEquals("company", FieldMatcher.mapToProfileKey("Company Name"))
        assertEquals("company", FieldMatcher.mapToProfileKey("Name of Employer"))
        assertEquals("emergencyContactName", FieldMatcher.mapToProfileKey("Emergency Contact Name"))
        assertEquals("emergencyContactPhone", FieldMatcher.mapToProfileKey("Emergency Contact Phone"))
        assertEquals("workEmail", FieldMatcher.mapToProfileKey("Work Email"))
        assertEquals("postalAddressLine1", FieldMatcher.mapToProfileKey("Mailing Address"))
    }

    @Test
    fun `a bare date field is not assumed to be the date of birth`() {
        assertEquals(FieldMatcher.UNKNOWN_KEY, FieldMatcher.mapToProfileKey("Date"))
    }

    @Test
    fun `unrecognised fields are unknown with low confidence`() {
        assertEquals(FieldMatcher.UNKNOWN_KEY, FieldMatcher.mapToProfileKey("Field_17"))
        val confidence = FieldMatcher.calculateConfidence("Field_17", FieldMatcher.UNKNOWN_KEY)
        assertEquals(ConfidenceLevel.LOW, FieldMatcher.confidenceLevel(confidence))
    }

    @Test
    fun `exact matches get high confidence`() {
        val confidence = FieldMatcher.calculateConfidence("first_name", "firstName")
        assertEquals(ConfidenceLevel.HIGH, FieldMatcher.confidenceLevel(confidence))
    }

    @Test
    fun `confidence levels use documented thresholds`() {
        assertEquals(ConfidenceLevel.HIGH, FieldMatcher.confidenceLevel(0.9f))
        assertEquals(ConfidenceLevel.MEDIUM, FieldMatcher.confidenceLevel(0.7f))
        assertEquals(ConfidenceLevel.LOW, FieldMatcher.confidenceLevel(0.69f))
    }

    @Test
    fun `identity numbers and birth date are sensitive`() {
        assertTrue(FieldMatcher.isSensitive("idNumber"))
        assertTrue(FieldMatcher.isSensitive("passportNumber"))
        assertTrue(FieldMatcher.isSensitive("dateOfBirth"))
        assertFalse(FieldMatcher.isSensitive("firstName"))
    }

    @Test
    fun `normalize splits camel case and punctuation`() {
        assertEquals("txt first name", FieldMatcher.normalize("txtFirstName"))
        assertEquals("zip code 0", FieldMatcher.normalize("zip_code[0]"))
    }
}
