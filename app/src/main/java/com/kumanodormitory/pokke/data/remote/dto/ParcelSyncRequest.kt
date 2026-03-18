package com.kumanodormitory.pokke.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParcelSyncRequest(val parcels: List<ParcelDto>)

@Serializable
data class ParcelDto(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ryoseiId: String,
    val ownerBlock: String,
    val ownerRoomName: String,
    val ownerName: String,
    val parcelType: String,
    val note: String? = null,
    val status: String,
    val isLost: Boolean,
    val registeredByName: String,
    val deliveredAt: Long? = null,
    val deliveredByName: String? = null,
    val lastConfirmedAt: Long? = null,
    val lostConfirmedAt: Long? = null
)
