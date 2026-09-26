package com.autodocfill.app.domain.pdf

import com.autodocfill.app.data.model.FieldMapping
import com.autodocfill.app.data.model.FieldType
import com.autodocfill.app.data.model.Profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillEngineTest {

    private val engine = AutofillEngine()
    private val profile = Profile(
        firstName = "Jane",
        lastName = "Doe",
        email = "not-an-email",
        company = "Acme",
        customFields = mapOf("maiden_name" to "Smith")
    )

    private fun field(key: String, type: FieldType = FieldType.TEXT) = FieldMapping(
        documentId = 1,
        fieldName = key,
        fieldType = type,
        fieldPage = 0,
        profileKey = key
    )

    @Test
    fun `fills values from the profile`() = runBlocking {
        val result = engine.autofillFields(listOf(field("firstName"), field("company"), field("maiden_name")), profile)
        assertEquals(listOf("Jane", "Acme", "Smith"), result.map { it.finalValue })
        assertTrue(result.all { it.isAutofilled })
    }

    @Test
    fun `builds full name without extra spaces`() {
        assertEquals("Jane Doe", engine.getProfileValue("fullName", profile))
    }

    @Test
    fun `invalid values are not filled and are flagged for review`() = runBlocking {
        val result = engine.autofillFields(listOf(field("email")), profile).single()
        assertEquals("", result.finalValue)
        assertFalse(result.isAutofilled)
        assertTrue(result.needsReview)
    }

    @Test
    fun `user edits and signature fields are left alone`() = runBlocking {
        val edited = field("firstName").copy(finalValue = "Janet", isUserEdited = true)
        val signature = field(FieldMatcher.SIGNATURE_KEY, FieldType.SIGNATURE)
        val result = engine.autofillFields(listOf(edited, signature), profile)
        assertEquals(listOf(edited, signature), result)
    }
}
