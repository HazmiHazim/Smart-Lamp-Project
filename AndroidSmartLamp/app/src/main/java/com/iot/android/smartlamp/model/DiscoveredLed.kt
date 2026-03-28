package com.iot.android.smartlamp.model

import java.util.UUID

data class DiscoveredLed(
    val serviceUUID: UUID,
    val rxUUID: UUID,
    val txUUID: UUID
)
