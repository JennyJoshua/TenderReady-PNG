package com.tenderready.png.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenderready.png.data.AppRepository
import com.tenderready.png.data.ReadinessItem
import com.tenderready.png.data.RequirementStatus
import com.tenderready.png.data.TenderRequirement
import kotlinx.coroutines.launch

/** Holds the repository and exposes database flows. Deletes and quick status changes run here. */
class MainViewModel(val repo: AppRepository) : ViewModel() {
    val profile = repo.profile
    val capabilities = repo.capabilityStatements
    val quotations = repo.quotations
    val opportunities = repo.opportunities
    val readiness = repo.readiness

    fun deleteCapability(id: Long) { viewModelScope.launch { repo.deleteCapability(id) } }
    fun deleteQuotation(id: Long) { viewModelScope.launch { repo.deleteQuotation(id) } }
    fun deleteOpportunity(id: Long) { viewModelScope.launch { repo.deleteOpportunity(id) } }
    fun deleteRequirement(id: Long) { viewModelScope.launch { repo.deleteRequirement(id) } }
    fun deleteReadinessItem(id: Long) { viewModelScope.launch { repo.deleteReadinessItem(id) } }
    fun setRequirementStatus(r: TenderRequirement, s: RequirementStatus) {
        viewModelScope.launch { repo.setRequirementStatus(r, s) }
    }
    fun saveReadinessItem(item: ReadinessItem) { viewModelScope.launch { repo.saveReadinessItem(item) } }
    fun ensureReadinessSeeded() { viewModelScope.launch { repo.ensureReadinessSeeded() } }
    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAllData()
            onDone()
        }
    }
}
