package com.drumpractise.app.analytics

import com.drumpractise.db.DrumDatabase

class AnalyticsRepository(
    private val db: DrumDatabase,
) {
    fun appendPracticeRoundCompleted(
        createdAt: Long,
        localUserId: String,
        subject: String,
        payloadSchema: String,
        payloadJson: String,
        appVersion: String? = null,
        appBuild: Long? = null,
    ) {
        db.practiceEventQueries.insertEvent(
            created_at = createdAt,
            local_user_id = localUserId,
            event_type = PracticeEventType.PRACTICE_ROUND_COMPLETED,
            subject = subject,
            payload_schema = payloadSchema,
            payload_json = payloadJson,
            app_version = appVersion,
            app_build = appBuild,
        )
    }

    fun countPracticeRoundsBySubjectBetween(
        subject: String,
        fromInclusive: Long,
        toInclusive: Long,
    ): Long =
        db.practiceEventQueries
            .countBySubjectBetween(
                subject = subject,
                created_at = fromInclusive,
                created_at_ = toInclusive,
            )
            .executeAsOne()

    fun listPracticeRoundsBySubjectBetween(
        subject: String,
        fromInclusive: Long,
        toInclusive: Long,
    ) =
        db.practiceEventQueries
            .selectBySubjectBetween(
                subject = subject,
                created_at = fromInclusive,
                created_at_ = toInclusive,
            )
            .executeAsList()
}

