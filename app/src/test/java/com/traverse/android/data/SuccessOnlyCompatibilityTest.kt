package com.traverse.android.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SuccessOnlyCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun installedAppAndCachedStatsFieldsRemainDecodable() {
        val stats = json.decodeFromString<SubmissionStats>(
            """{"stats":{"total":8,"accepted":8,"failed":0,"acceptanceRate":"100.00%","languageBreakdown":[]}}"""
        )
        assertEquals(8, stats.stats.total)
        assertEquals(8, stats.stats.accepted)
    }

    @Test
    fun retiredLapseFieldsDoNotAffectRetentionDecoding() {
        val common = """"problemId":1,"problemTitle":"Two Sum","problemSlug":"two-sum","platform":"leetcode","difficulty":"easy","retrievability":0.8,"stability":10,"difficulty_D":3"""
        val legacy = json.decodeFromString<RevisionRetentionItem>("{$common,\"lapses\":9,\"isLeech\":true}")
        val current = json.decodeFromString<RevisionRetentionItem>("{$common,\"lapses\":0,\"isLeech\":false}")
        val minimal = json.decodeFromString<RevisionRetentionItem>("{$common}")
        assertEquals(minimal, legacy)
        assertEquals(minimal, current)
    }
}
