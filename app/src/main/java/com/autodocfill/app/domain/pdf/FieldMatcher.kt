package com.autodocfill.app.domain.pdf

import com.autodocfill.app.data.model.ConfidenceLevel

/**
 * Maps PDF field names / OCR labels to profile keys and scores the match.
 * Pure Kotlin so it can be unit tested without Android.
 */
object FieldMatcher {

    const val UNKNOWN_KEY = "unknown"
    const val SIGNATURE_KEY = "signature"

    private val sensitiveKeys = setOf(
        "idNumber", "passportNumber", "driverLicenseNumber",
        "taxNumber", "dateOfBirth"
    )

    /**
     * Ordered rules: the first matching rule wins, so more specific rules
     * (e.g. "company name", "emergency contact phone") must come before
     * generic ones (e.g. "name", "phone").
     */
    private val rules: List<Pair<(String) -> Boolean, String>> = listOf(
        // Signature
        { n: String -> n.has("signature") || n.has("sign here") } to SIGNATURE_KEY,

        // Emergency contact (before generic name/phone)
        { n: String -> n.has("emergency") && n.has("phone", "tel", "cell", "mobile") } to "emergencyContactPhone",
        { n: String -> n.has("emergency") && n.has("relation") } to "emergencyContactRelation",
        { n: String -> n.has("emergency") } to "emergencyContactName",

        // Employment (before generic name/address/phone/email)
        { n: String -> n.has("employer", "company", "organisation", "organization") } to "company",
        { n: String -> n.has("work", "business", "office") && n.has("email", "e-mail") } to "workEmail",
        { n: String -> n.has("work", "business", "office") && n.has("phone", "tel") } to "workPhone",
        { n: String -> n.has("job title", "jobtitle", "position", "designation") } to "jobTitle",
        { n: String -> n.has("occupation", "profession", "job") } to "occupation",
        { n: String -> n.has("income", "salary") } to "annualIncome",

        // Name
        { n: String -> n.has("first", "given", "fname") && n.has("name", "fname") } to "firstName",
        { n: String -> n.has("middle") && n.has("name") } to "middleName",
        { n: String -> n.has("last", "surname", "family", "lname") } to "lastName",
        { n: String -> n.has("full") && n.has("name") } to "fullName",

        // Contact
        { n: String -> n.has("email", "e-mail") } to "email",
        { n: String -> n.has("alternate", "alternative", "other") && n.has("phone", "tel", "number") } to "alternatePhone",
        { n: String -> n.has("phone", "tel", "mobile", "cell", "contact number") } to "phoneNumber",

        // Postal address (before residential)
        { n: String -> n.has("postal", "mailing") && n.has("address") && n.has("2") } to "postalAddressLine2",
        { n: String -> n.has("postal", "mailing") && n.has("address") } to "postalAddressLine1",
        { n: String -> n.has("postal code", "postcode", "zip") } to "zipCode",

        // Residential address
        { n: String -> n.has("address") && n.has("2") } to "addressLine2",
        { n: String -> n.has("address", "street") } to "addressLine1",
        { n: String -> n.has("city", "town") } to "city",
        { n: String -> n.has("state", "province", "region") } to "state",
        { n: String -> n.has("country") } to "country",

        // Identification
        { n: String -> n.has("ssn", "social security", "id number", "idnumber", "national id", "identity") } to "idNumber",
        { n: String -> n.has("passport") } to "passportNumber",
        { n: String -> n.has("driver", "licence", "license") } to "driverLicenseNumber",
        { n: String -> n.has("tax") } to "taxNumber",

        // Personal
        { n: String -> n.has("birth", "dob") } to "dateOfBirth",
        { n: String -> n.has("gender", "sex") } to "gender",
        { n: String -> n.has("nationality", "citizenship") } to "nationality",

        // Generic "name" last, so e.g. "company name" is not treated as a person's name
        { n: String -> n.has("name") } to "fullName"
    )

    /**
     * Map a PDF field name (e.g. "applicant_first_name", "txtEmail") to a profile key,
     * or [UNKNOWN_KEY] when nothing matches.
     */
    fun mapToProfileKey(fieldName: String): String {
        val normalized = normalize(fieldName)
        return rules.firstOrNull { (matches, _) -> matches(normalized) }?.second ?: UNKNOWN_KEY
    }

    /**
     * Confidence score (0.0-1.0) for mapping [fieldName] to [profileKey].
     */
    fun calculateConfidence(fieldName: String, profileKey: String): Float {
        if (profileKey == UNKNOWN_KEY) return 0.3f

        val normalized = normalize(fieldName)
        val keyWords = profileKey.split(Regex("(?=[A-Z])")).map { it.lowercase() }

        var confidence = 0.5f
        keyWords.forEach { keyword ->
            if (normalized.contains(keyword)) confidence += 0.2f
        }
        if (normalized.startsWith(keyWords.first())) confidence += 0.1f

        return confidence.coerceIn(0f, 1f)
    }

    fun confidenceLevel(score: Float): ConfidenceLevel = when {
        score >= 0.9f -> ConfidenceLevel.HIGH
        score >= 0.7f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }

    fun isSensitive(profileKey: String): Boolean = profileKey in sensitiveKeys

    /**
     * Lowercase and split camelCase / snake_case / punctuation into spaced words,
     * e.g. "txtFirstName" -> "txt first name", "zip_code[0]" -> "zip code 0".
     */
    internal fun normalize(fieldName: String): String =
        fieldName
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .lowercase()
            .replace(Regex("[^a-z0-9-]+"), " ")
            .trim()

    private fun String.has(vararg terms: String): Boolean = terms.any { contains(it) }
}
