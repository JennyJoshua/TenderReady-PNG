package com.tenderready.png.data

object ReadinessDefaults {
    val categories = listOf(
        "Business identity and registration",
        "Tax information",
        "Banking and payment information",
        "Licences and permits",
        "Experience and references",
        "Insurance and certifications",
        "Business contact and capability information"
    )

    /** Generic starting prompts only. Real requirements vary by business, industry and opportunity. */
    val items: List<Pair<String, String>> = listOf(
        categories[0] to "Business registration documents, if they apply to your business",
        categories[0] to "Identification for the business owner or directors",
        categories[1] to "Tax identification details, if they apply to your business",
        categories[1] to "Any tax documents a client may ask you to show",
        categories[2] to "Business bank account details ready for payments",
        categories[2] to "Payment terms you are happy to offer clients",
        categories[3] to "Licences or permits needed for your type of work, if any",
        categories[4] to "List of previous jobs or clients",
        categories[4] to "Referees who agree to be named",
        categories[5] to "Insurance cover, if relevant to your work",
        categories[5] to "Certificates or qualifications, if relevant",
        categories[6] to "Phone and email you check regularly",
        categories[6] to "Short description of what your business offers"
    )
}
