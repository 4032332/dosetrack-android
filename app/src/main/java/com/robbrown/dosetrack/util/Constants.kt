package com.robbrown.dosetrack.util

/**
 * App-wide constants shared with the iOS app so behaviour matches across platforms.
 */
object Constants {
    const val APPLICATION_ID = "com.robbrown.dosetrack"

    // StoreKit / Play Billing product IDs — identical to the iOS Products.storekit.
    const val PRODUCT_MONTHLY = "com.robbrown.dosetrack.pro.monthly"
    const val PRODUCT_ANNUAL = "com.robbrown.dosetrack.pro.annual"

    // Free-tier cap: 5 medications free forever (Pro removes the limit).
    const val FREE_TIER_MED_LIMIT = 5
}
