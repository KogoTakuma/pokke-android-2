package com.kumanodormitory.pokke.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parcels")
data class ParcelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientName: String,
    val roomNumber: String,
    val parcelType: String,
    val note: String = "",
    val registeredBy: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val releasedAt: Long? = null,
    val releasedTo: String? = null,
    val isProxy: Boolean = false,
    val status: String = "registered" // registered, released
)
