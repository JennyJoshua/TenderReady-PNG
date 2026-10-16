package com.tenderready.png.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Room runs on the JVM here through Robolectric, using an in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private var now = 1_000L

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = AppRepository(db) { now++ }
    }

    @After fun tearDown() {
        db.close()
    }

    private fun count(table: String): Int {
        val c = db.query("SELECT COUNT(*) FROM $table", null)
        return c.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    private fun quote(number: String = "QT-0001", client: String = "Client") =
        Quotation(quotationNumber = number, clientName = client, issueDate = "2026-10-17", businessName = "Biz")

    private fun item(desc: String, price: Long = 1000L) =
        QuotationItem(description = desc, quantityMilli = 2000L, unit = "day", unitPriceMinor = price)

    // ---- Profile
    @Test fun createRetrieveAndEditProfile() = runBlocking {
        assertNull(repo.getProfile())
        repo.saveProfile(BusinessProfile(businessName = "Gulf Builders", phone = "123456"))
        assertEquals("Gulf Builders", repo.getProfile()?.businessName)
        repo.saveProfile(repo.getProfile()!!.copy(businessName = "Gulf Builders Ltd", location = "Baimuru"))
        val p = repo.profile.first()
        assertEquals("Gulf Builders Ltd", p?.businessName)
        assertEquals("Baimuru", p?.location)
        assertEquals(1, count("business_profile"))
    }

    @Test fun editingProfileDoesNotUnlinkSavedDocuments() = runBlocking {
        repo.saveProfile(BusinessProfile(businessName = "A"))
        val id = (repo.saveQuotation(quote(), listOf(item("x"))) as SaveResult.Saved).id
        repo.saveProfile(BusinessProfile(businessName = "B"))
        assertEquals(1L, repo.getQuotation(id)?.quotation?.profileId)
    }

    // ---- Capability statements
    @Test fun saveAndEditCapabilityStatement() = runBlocking {
        val id = (repo.saveCapability(CapabilityStatement(businessName = "A", overview = "o")) as SaveResult.Saved).id
        assertEquals("o", repo.getCapability(id)?.overview)
        repo.saveCapability(repo.getCapability(id)!!.copy(overview = "changed"))
        assertEquals("changed", repo.getCapability(id)?.overview)
        assertEquals(1, repo.capabilityStatements.first().size)
        repo.deleteCapability(id)
        assertNull(repo.getCapability(id))
    }

    // ---- Quotations
    @Test fun saveAndRetrieveQuotationWithItemsInOrder() = runBlocking {
        val id = (repo.saveQuotation(quote(), listOf(item("first"), item("second"), item("third"))) as SaveResult.Saved).id
        val got = repo.getQuotation(id)!!
        assertEquals("QT-0001", got.quotation.quotationNumber)
        assertEquals(listOf("first", "second", "third"), got.items.sortedBy { it.position }.map { it.description })
        assertEquals(3, count("quotation_item"))
    }

    @Test fun editingQuotationReplacesItemsAndKeepsOneRecord() = runBlocking {
        val id = (repo.saveQuotation(quote(), listOf(item("a"), item("b"))) as SaveResult.Saved).id
        val existing = repo.getQuotation(id)!!.quotation
        repo.saveQuotation(existing.copy(clientName = "New client"), listOf(item("only")))
        assertEquals(1, count("quotation"))
        assertEquals(1, count("quotation_item"))
        assertEquals("New client", repo.getQuotation(id)?.quotation?.clientName)
    }

    @Test fun duplicateQuotationNumberIsRejectedAndNothingIsOverwritten() = runBlocking {
        repo.saveQuotation(quote("QT-0001", "First"), listOf(item("a")))
        val second = repo.saveQuotation(quote("qt-0001", "Second"), listOf(item("b")))
        assertTrue(second is SaveResult.Failed)
        assertEquals(1, count("quotation"))
        assertEquals("First", repo.quotations.first().single().quotation.clientName)
    }

    @Test fun editingAnotherQuotationToAnExistingNumberIsRejected() = runBlocking {
        repo.saveQuotation(quote("QT-0001"), listOf(item("a")))
        val id2 = (repo.saveQuotation(quote("QT-0002"), listOf(item("b"))) as SaveResult.Saved).id
        val r = repo.saveQuotation(repo.getQuotation(id2)!!.quotation.copy(quotationNumber = "QT-0001"), listOf(item("b")))
        assertTrue(r is SaveResult.Failed)
        assertEquals("QT-0002", repo.getQuotation(id2)?.quotation?.quotationNumber)
    }

    @Test fun resavingWithSameNumberIsAllowed() = runBlocking {
        val id = (repo.saveQuotation(quote(), listOf(item("a"))) as SaveResult.Saved).id
        val r = repo.saveQuotation(repo.getQuotation(id)!!.quotation, listOf(item("a")))
        assertTrue(r is SaveResult.Saved)
    }

    @Test fun nextQuotationNumberSkipsUsedNumbers() = runBlocking {
        assertEquals("QT-0001", repo.nextQuotationNumber())
        repo.saveQuotation(quote("QT-0002"), listOf(item("a"))) // count=1, so next guess QT-0002 is taken
        assertEquals("QT-0003", repo.nextQuotationNumber())
    }

    @Test fun deletingQuotationRemovesItsItems() = runBlocking {
        val id = (repo.saveQuotation(quote(), listOf(item("a"), item("b"))) as SaveResult.Saved).id
        val other = (repo.saveQuotation(quote("QT-0002"), listOf(item("c"))) as SaveResult.Saved).id
        repo.deleteQuotation(id)
        assertNull(repo.getQuotation(id))
        assertEquals(1, count("quotation_item"))
        assertNotNull(repo.getQuotation(other))
    }

    // ---- Tenders
    @Test fun saveOpportunityAndRequirements() = runBlocking {
        val oid = (repo.saveOpportunity(TenderOpportunity(title = "Road repair", issuer = "Council")) as SaveResult.Saved).id
        val r1 = (repo.saveRequirement(TenderRequirement(opportunityId = oid, title = "Tax letter")) as SaveResult.Saved).id
        repo.saveRequirement(TenderRequirement(opportunityId = oid, title = "Bank details"))
        val got = repo.getOpportunity(oid)!!
        assertEquals("Road repair", got.opportunity.title)
        assertEquals(2, got.requirements.size)

        repo.setRequirementStatus(got.requirements.first { it.id == r1 }, RequirementStatus.COMPLETE)
        val after = repo.getOpportunity(oid)!!.requirements
        assertEquals(RequirementStatus.COMPLETE.name, after.first { it.id == r1 }.status)

        repo.deleteRequirement(r1)
        assertEquals(1, repo.getOpportunity(oid)!!.requirements.size)
    }

    @Test fun deletingOpportunityRemovesRequirements() = runBlocking {
        val oid = (repo.saveOpportunity(TenderOpportunity(title = "A")) as SaveResult.Saved).id
        val keep = (repo.saveOpportunity(TenderOpportunity(title = "B")) as SaveResult.Saved).id
        repo.saveRequirement(TenderRequirement(opportunityId = oid, title = "r1"))
        repo.saveRequirement(TenderRequirement(opportunityId = keep, title = "r2"))
        repo.deleteOpportunity(oid)
        assertEquals(1, count("tender_requirement"))
        assertEquals(1, repo.opportunities.first().size)
    }

    @Test fun requirementForMissingOpportunityFailsCleanly() = runBlocking {
        val r = repo.saveRequirement(TenderRequirement(opportunityId = 999, title = "orphan"))
        assertTrue(r is SaveResult.Failed)
        assertEquals(0, count("tender_requirement"))
    }

    // ---- Readiness
    @Test fun readinessIsSeededOnlyOnce() = runBlocking {
        repo.ensureReadinessSeeded()
        repo.ensureReadinessSeeded()
        assertEquals(ReadinessDefaults.items.size, repo.readiness.first().size)
    }

    @Test fun readinessItemsCanBeAddedEditedAndRemoved() = runBlocking {
        repo.ensureReadinessSeeded()
        repo.saveReadinessItem(ReadinessItem(category = "Tax information", title = "My own item"))
        val mine = repo.readiness.first().first { it.title == "My own item" }
        repo.saveReadinessItem(mine.copy(status = ReadinessStatus.IN_PROGRESS.name, notes = "call office"))
        assertEquals(ReadinessStatus.IN_PROGRESS.name, repo.readiness.first().first { it.id == mine.id }.status)
        repo.deleteReadinessItem(mine.id)
        assertEquals(ReadinessDefaults.items.size, repo.readiness.first().size)
    }

    // ---- Delete everything
    @Test fun deleteAllDataEmptiesEveryTable() = runBlocking {
        repo.saveProfile(BusinessProfile(businessName = "A"))
        repo.saveCapability(CapabilityStatement(businessName = "A"))
        repo.saveQuotation(quote(), listOf(item("a")))
        val oid = (repo.saveOpportunity(TenderOpportunity(title = "T")) as SaveResult.Saved).id
        repo.saveRequirement(TenderRequirement(opportunityId = oid, title = "r"))
        repo.ensureReadinessSeeded()
        repo.deleteAllData()
        listOf(
            "business_profile", "capability_statement", "quotation", "quotation_item",
            "tender_opportunity", "tender_requirement", "readiness_item"
        ).forEach { assertEquals(it, 0, count(it)) }
    }
}
