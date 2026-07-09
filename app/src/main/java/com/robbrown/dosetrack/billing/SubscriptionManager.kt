package com.robbrown.dosetrack.billing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Pro subscription via Google Play Billing (monthly / annual products).
 *
 * Implemented in the billing phase. Free-tier entitlement is cached for offline access;
 * product IDs live in [com.robbrown.dosetrack.util.Constants].
 */
@Singleton
class SubscriptionManager @Inject constructor()
