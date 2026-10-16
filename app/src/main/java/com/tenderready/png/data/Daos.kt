package com.tenderready.png.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun observe(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun get(): BusinessProfile?

    // Upsert (not REPLACE) so saving the profile never deletes the row and triggers SET_NULL on documents.
    @Upsert
    suspend fun upsert(profile: BusinessProfile)
}

@Dao
interface CapabilityDao {
    @Query("SELECT * FROM capability_statement ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CapabilityStatement>>

    @Query("SELECT * FROM capability_statement WHERE id = :id")
    suspend fun get(id: Long): CapabilityStatement?

    @Insert
    suspend fun insert(c: CapabilityStatement): Long

    @Update
    suspend fun update(c: CapabilityStatement)

    @Query("DELETE FROM capability_statement WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QuotationDao {
    @Transaction
    @Query("SELECT * FROM quotation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuotationWithItems>>

    @Transaction
    @Query("SELECT * FROM quotation WHERE id = :id")
    suspend fun get(id: Long): QuotationWithItems?

    @Query("SELECT COUNT(*) FROM quotation WHERE quotationNumber = :number AND id != :excludeId")
    suspend fun countNumber(number: String, excludeId: Long): Int

    @Query("SELECT COUNT(*) FROM quotation")
    suspend fun count(): Int

    @Insert
    suspend fun insert(q: Quotation): Long

    @Update
    suspend fun update(q: Quotation)

    @Insert
    suspend fun insertItems(items: List<QuotationItem>)

    @Query("DELETE FROM quotation_item WHERE quotationId = :id")
    suspend fun deleteItems(id: Long)

    @Query("DELETE FROM quotation WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TenderDao {
    @Transaction
    @Query("SELECT * FROM tender_opportunity ORDER BY updatedAt DESC")
    fun observeOpportunities(): Flow<List<OpportunityWithRequirements>>

    @Transaction
    @Query("SELECT * FROM tender_opportunity WHERE id = :id")
    fun observeOpportunity(id: Long): Flow<OpportunityWithRequirements?>

    @Transaction
    @Query("SELECT * FROM tender_opportunity WHERE id = :id")
    suspend fun getOpportunity(id: Long): OpportunityWithRequirements?

    @Insert
    suspend fun insertOpportunity(o: TenderOpportunity): Long

    @Update
    suspend fun updateOpportunity(o: TenderOpportunity)

    @Query("DELETE FROM tender_opportunity WHERE id = :id")
    suspend fun deleteOpportunity(id: Long)

    @Insert
    suspend fun insertRequirement(r: TenderRequirement): Long

    @Update
    suspend fun updateRequirement(r: TenderRequirement)

    @Query("DELETE FROM tender_requirement WHERE id = :id")
    suspend fun deleteRequirement(id: Long)

    @Query("SELECT COUNT(*) FROM tender_requirement WHERE opportunityId = :opportunityId")
    suspend fun countRequirements(opportunityId: Long): Int
}

@Dao
interface ReadinessDao {
    @Query("SELECT * FROM readiness_item ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<ReadinessItem>>

    @Query("SELECT * FROM readiness_item ORDER BY sortOrder, id")
    suspend fun getAll(): List<ReadinessItem>

    @Query("SELECT COUNT(*) FROM readiness_item")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: ReadinessItem): Long

    @Insert
    suspend fun insertAll(items: List<ReadinessItem>)

    @Update
    suspend fun update(item: ReadinessItem)

    @Query("DELETE FROM readiness_item WHERE id = :id")
    suspend fun delete(id: Long)
}
