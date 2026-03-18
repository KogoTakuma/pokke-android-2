package com.kumanodormitory.pokke.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kumanodormitory.pokke.data.remote.dto.ParcelDto

@Entity(tableName = "parcels")
data class ParcelEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // 受け取り主
    @ColumnInfo(name = "ryosei_id")
    val ryoseiId: String,
    @ColumnInfo(name = "owner_block")
    val ownerBlock: String,
    @ColumnInfo(name = "owner_room_name")
    val ownerRoomName: String,
    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    // 荷物情報
    @ColumnInfo(name = "parcel_type")
    val parcelType: String,
    val note: String? = null,

    // 状態
    val status: String = ParcelStatus.REGISTERED.name,
    @ColumnInfo(name = "is_lost")
    val isLost: Boolean = false,

    // 登録情報
    @ColumnInfo(name = "registered_by_name")
    val registeredByName: String,

    // 引渡情報
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long? = null,
    @ColumnInfo(name = "delivered_by_name")
    val deliveredByName: String? = null,

    // 同期
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,
    @ColumnInfo(name = "device_id")
    val deviceId: String? = null,

    // 泊まり事務当番
    @ColumnInfo(name = "last_confirmed_at")
    val lastConfirmedAt: Long? = null,

    // 紛失確定
    @ColumnInfo(name = "lost_confirmed_at", defaultValue = "NULL")
    val lostConfirmedAt: Long? = null
) {
    fun toSyncDto() = ParcelDto(
        id = id, createdAt = createdAt, updatedAt = updatedAt,
        ryoseiId = ryoseiId, ownerBlock = ownerBlock,
        ownerRoomName = ownerRoomName, ownerName = ownerName,
        parcelType = parcelType, note = note, status = status,
        isLost = isLost, registeredByName = registeredByName,
        deliveredAt = deliveredAt, deliveredByName = deliveredByName,
        lastConfirmedAt = lastConfirmedAt, lostConfirmedAt = lostConfirmedAt
    )
}
