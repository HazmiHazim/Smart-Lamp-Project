package com.iot.android.smartlamp.model

import java.util.UUID

data class LampBluetoothAddress(
    val lampId : String,
    val serviceAddress : UUID,
    val transmitterAddress : UUID,
    val receiverAddress : UUID
)