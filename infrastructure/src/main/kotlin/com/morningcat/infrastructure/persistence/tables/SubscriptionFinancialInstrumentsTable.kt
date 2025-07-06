package com.morningcat.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

object SubscriptionFinancialInstrumentsTable : Table("subscription_financial_instruments") {
    val userId = uuid("user_id") references SubscriptionsTable.userId
    val ticker = varchar("ticker", 10)

    override val primaryKey = PrimaryKey(userId, ticker)

    init {
        index(false, userId)
    }
}
