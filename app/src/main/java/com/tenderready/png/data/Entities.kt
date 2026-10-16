package com.tenderready.png.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class RequirementStatus(val label: String) {
    OUTSTANDING("Outstanding"), COMPLETE("Complete"), NOT_APPLICABLE("Not applicable");

    companion object {
        fun from(name: String): RequirementStatus = values().firstOrNull { it.name == name } ?: OUTSTANDING
    }
}

enum class ReadinessStatus(val label: String) {
    NOT_STARTED("Not started"), IN_PROGRESS("In progress"), COMPLETE("Complete"), NOT_APPLICABLE("Not applicable");

    companion object {
        fun from(name: String): ReadinessStatus = values().firstOrNull { it.name == name } ?: NOT_STARTED
    }
}

/** One profile per device (id is always 1). Timestamps are epoch milliseconds (UTC). */
@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Long = 1L,
    val businessName: String = "",
    val businessType: String = "",
    val location: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val productsServices: String = "",
    val areasServed: String = "",
    val yearsExperience: String = "",
    val registrationStatus: String = "",
    val tinStatus: String = "",
    val qualifications: String = "",
    val references: String = "",
    val updatedAt: Long = 0L
)

/**
 * Capability statements keep their own editable copy (snapshot) of the business text, so editing
 * the profile later never silently changes an older statement. profileId is a link only.
 */
@Entity(
    tableName = "capability_statement",
    foreignKeys = [ForeignKey(
        entity = BusinessProfile::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("profileId")]
)
data class CapabilityStatement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long? = null,
    val businessName: String = "",
    val overview: String = "",
    val productsServices: String = "",
    val experience: String = "",
    val qualifications: String = "",
    val references: String = "",
    val contactDetails: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** Quotations also snapshot the business details that were on the quotation when it was saved. */
@Entity(
    tableName = "quotation",
    foreignKeys = [ForeignKey(
        entity = BusinessProfile::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("profileId"), Index(value = ["quotationNumber"], unique = true)]
)
data class Quotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val quotationNumber: String = "",
    val profileId: Long? = null,
    val businessName: String = "",
    val businessLocation: String = "",
    val businessContact: String = "",
    val issueDate: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val workDescription: String = "",
    val additionalChargesLabel: String = "",
    val additionalChargesMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val taxEnabled: Boolean = false,
    val taxLabel: String = "",
    val taxRateBp: Int = 0,
    val validityDays: Int = 0,
    val paymentTerms: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "quotation_item",
    foreignKeys = [ForeignKey(
        entity = Quotation::class,
        parentColumns = ["id"],
        childColumns = ["quotationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("quotationId")]
)
data class QuotationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val quotationId: Long = 0L,
    val position: Int = 0,
    val description: String = "",
    val quantityMilli: Long = 1000L,
    val unit: String = "",
    val unitPriceMinor: Long = 0L
)

data class QuotationWithItems(
    @Embedded val quotation: Quotation,
    @Relation(parentColumn = "id", entityColumn = "quotationId") val items: List<QuotationItem>
)

@Entity(tableName = "tender_opportunity")
data class TenderOpportunity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val issuer: String = "",
    val referenceNumber: String = "",
    val deadline: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "tender_requirement",
    foreignKeys = [ForeignKey(
        entity = TenderOpportunity::class,
        parentColumns = ["id"],
        childColumns = ["opportunityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("opportunityId")]
)
data class TenderRequirement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val opportunityId: Long = 0L,
    val title: String = "",
    val notes: String = "",
    val status: String = RequirementStatus.OUTSTANDING.name,
    val deadline: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class OpportunityWithRequirements(
    @Embedded val opportunity: TenderOpportunity,
    @Relation(parentColumn = "id", entityColumn = "opportunityId") val requirements: List<TenderRequirement>
)

@Entity(tableName = "readiness_item")
data class ReadinessItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String = "",
    val title: String = "",
    val notes: String = "",
    val status: String = ReadinessStatus.NOT_STARTED.name,
    val sortOrder: Int = 0,
    val updatedAt: Long = 0L
)
