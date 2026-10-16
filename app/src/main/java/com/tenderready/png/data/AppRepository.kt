package com.tenderready.png.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

sealed class SaveResult {
    data class Saved(val id: Long) : SaveResult()
    data class Failed(val message: String) : SaveResult()
}

/** The database is the source of truth. Screens only read and write through this class. */
class AppRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    // ---- Business profile
    val profile: Flow<BusinessProfile?> = db.profileDao().observe()
    suspend fun getProfile(): BusinessProfile? = db.profileDao().get()
    suspend fun saveProfile(p: BusinessProfile) {
        db.profileDao().upsert(p.copy(id = 1L, updatedAt = clock()))
    }

    private suspend fun profileIdOrNull(): Long? = if (db.profileDao().get() != null) 1L else null

    // ---- Capability statements
    val capabilityStatements: Flow<List<CapabilityStatement>> = db.capabilityDao().observeAll()
    suspend fun getCapability(id: Long): CapabilityStatement? = db.capabilityDao().get(id)
    suspend fun saveCapability(c: CapabilityStatement): SaveResult {
        val now = clock()
        val pid = profileIdOrNull()
        return if (c.id == 0L) {
            SaveResult.Saved(db.capabilityDao().insert(c.copy(profileId = pid, createdAt = now, updatedAt = now)))
        } else {
            db.capabilityDao().update(c.copy(profileId = pid, updatedAt = now))
            SaveResult.Saved(c.id)
        }
    }
    suspend fun deleteCapability(id: Long) = db.capabilityDao().delete(id)

    // ---- Quotations
    val quotations: Flow<List<QuotationWithItems>> = db.quotationDao().observeAll()
    suspend fun getQuotation(id: Long): QuotationWithItems? = db.quotationDao().get(id)

    /** Suggests "QT-0001", "QT-0002"... skipping numbers already used. */
    suspend fun nextQuotationNumber(): String {
        var n = db.quotationDao().count() + 1
        while (true) {
            val candidate = "QT-" + n.toString().padStart(4, '0')
            if (db.quotationDao().countNumber(candidate, 0L) == 0) return candidate
            n++
        }
    }

    suspend fun saveQuotation(q: Quotation, items: List<QuotationItem>): SaveResult {
        val number = q.quotationNumber.trim().uppercase()
        if (number.isEmpty()) return SaveResult.Failed("Quotation number is required.")
        if (db.quotationDao().countNumber(number, q.id) > 0) {
            return SaveResult.Failed("Quotation number $number is already used. Choose a different number.")
        }
        val pid = profileIdOrNull()
        return try {
            val id = db.withTransaction {
                val now = clock()
                val savedId = if (q.id == 0L) {
                    db.quotationDao().insert(q.copy(quotationNumber = number, profileId = pid, createdAt = now, updatedAt = now))
                } else {
                    db.quotationDao().update(q.copy(quotationNumber = number, profileId = pid, updatedAt = now))
                    db.quotationDao().deleteItems(q.id)
                    q.id
                }
                db.quotationDao().insertItems(
                    items.mapIndexed { i, it -> it.copy(id = 0L, quotationId = savedId, position = i) }
                )
                savedId
            }
            SaveResult.Saved(id)
        } catch (e: SQLiteConstraintException) {
            SaveResult.Failed("Could not save. The number may already be used, or this quotation was deleted.")
        }
    }
    suspend fun deleteQuotation(id: Long) = db.quotationDao().delete(id)

    // ---- Tender opportunities and requirements
    val opportunities: Flow<List<OpportunityWithRequirements>> = db.tenderDao().observeOpportunities()
    fun observeOpportunity(id: Long): Flow<OpportunityWithRequirements?> =
        if (id == 0L) flowOf(null) else db.tenderDao().observeOpportunity(id)
    suspend fun getOpportunity(id: Long): OpportunityWithRequirements? = db.tenderDao().getOpportunity(id)

    suspend fun saveOpportunity(o: TenderOpportunity): SaveResult {
        val now = clock()
        return if (o.id == 0L) {
            SaveResult.Saved(db.tenderDao().insertOpportunity(o.copy(createdAt = now, updatedAt = now)))
        } else {
            db.tenderDao().updateOpportunity(o.copy(updatedAt = now))
            SaveResult.Saved(o.id)
        }
    }
    suspend fun deleteOpportunity(id: Long) = db.tenderDao().deleteOpportunity(id)

    suspend fun saveRequirement(r: TenderRequirement): SaveResult {
        val now = clock()
        return try {
            if (r.id == 0L) {
                SaveResult.Saved(db.tenderDao().insertRequirement(r.copy(createdAt = now, updatedAt = now)))
            } else {
                db.tenderDao().updateRequirement(r.copy(updatedAt = now))
                SaveResult.Saved(r.id)
            }
        } catch (e: SQLiteConstraintException) {
            SaveResult.Failed("Could not save. The opportunity may have been deleted.")
        }
    }
    suspend fun setRequirementStatus(r: TenderRequirement, status: RequirementStatus) {
        db.tenderDao().updateRequirement(r.copy(status = status.name, updatedAt = clock()))
    }
    suspend fun deleteRequirement(id: Long) = db.tenderDao().deleteRequirement(id)

    // ---- Readiness checklist
    val readiness: Flow<List<ReadinessItem>> = db.readinessDao().observeAll()

    /** Adds the generic starter items the first time the checklist is opened. */
    suspend fun ensureReadinessSeeded() {
        db.withTransaction {
            if (db.readinessDao().count() == 0) {
                val now = clock()
                db.readinessDao().insertAll(
                    ReadinessDefaults.items.mapIndexed { i, (cat, title) ->
                        ReadinessItem(category = cat, title = title, sortOrder = i, updatedAt = now)
                    }
                )
            }
        }
    }
    suspend fun saveReadinessItem(item: ReadinessItem) {
        val now = clock()
        if (item.id == 0L) {
            val order = db.readinessDao().getAll().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
            db.readinessDao().insert(item.copy(sortOrder = order, updatedAt = now))
        } else {
            db.readinessDao().update(item.copy(updatedAt = now))
        }
    }
    suspend fun deleteReadinessItem(id: Long) = db.readinessDao().delete(id)

    // ---- Settings: delete everything stored by the app
    suspend fun deleteAllData() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
