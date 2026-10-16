package com.tenderready.png.core

import com.tenderready.png.data.ReadinessItem
import com.tenderready.png.data.ReadinessStatus
import com.tenderready.png.data.RequirementStatus
import com.tenderready.png.data.TenderRequirement
import com.tenderready.png.data.readinessProgress
import com.tenderready.png.data.requirementProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistTest {
    private fun req(s: RequirementStatus) = TenderRequirement(title = "r", status = s.name)

    @Test fun emptyChecklist() {
        val p = requirementProgress(emptyList())
        assertEquals(0, p.total)
        assertEquals(0, p.percent)
        assertEquals(0, p.outstanding)
    }

    @Test fun allOutstanding() {
        val p = requirementProgress(List(4) { req(RequirementStatus.OUTSTANDING) })
        assertEquals(0, p.percent)
        assertEquals(4, p.outstanding)
    }

    @Test fun allApplicableComplete() {
        val p = requirementProgress(List(3) { req(RequirementStatus.COMPLETE) })
        assertEquals(100, p.percent)
        assertEquals(0, p.outstanding)
    }

    @Test fun notApplicableIsExcluded() {
        val p = requirementProgress(
            listOf(
                req(RequirementStatus.COMPLETE), req(RequirementStatus.COMPLETE),
                req(RequirementStatus.OUTSTANDING), req(RequirementStatus.OUTSTANDING),
                req(RequirementStatus.NOT_APPLICABLE)
            )
        )
        assertEquals(5, p.total)
        assertEquals(2, p.complete)
        assertEquals(1, p.notApplicable)
        assertEquals(2, p.outstanding)
        assertEquals(50, p.percent)
    }

    @Test fun allNotApplicableIsZeroPercent() {
        val p = requirementProgress(List(2) { req(RequirementStatus.NOT_APPLICABLE) })
        assertEquals(0, p.percent)
        assertEquals(0, p.outstanding)
    }

    @Test fun percentRoundsDown() {
        assertEquals(33, ChecklistProgress(total = 3, complete = 1, notApplicable = 0).percent)
        assertEquals(66, ChecklistProgress(total = 3, complete = 2, notApplicable = 0).percent)
    }

    @Test fun readinessInProgressCountsAsOutstanding() {
        val items = listOf(
            ReadinessItem(title = "a", status = ReadinessStatus.COMPLETE.name),
            ReadinessItem(title = "b", status = ReadinessStatus.IN_PROGRESS.name),
            ReadinessItem(title = "c", status = ReadinessStatus.NOT_STARTED.name),
            ReadinessItem(title = "d", status = ReadinessStatus.NOT_APPLICABLE.name)
        )
        val p = readinessProgress(items)
        assertEquals(33, p.percent)
        assertEquals(2, p.outstanding)
    }

    @Test fun unknownStatusFallsBackSafely() {
        assertEquals(RequirementStatus.OUTSTANDING, RequirementStatus.from("garbage"))
        assertEquals(ReadinessStatus.NOT_STARTED, ReadinessStatus.from("garbage"))
    }
}
