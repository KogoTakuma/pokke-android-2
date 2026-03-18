package com.kumanodormitory.pokke.data.remote

import com.kumanodormitory.pokke.data.remote.dto.CallRequest
import com.kumanodormitory.pokke.data.remote.dto.ParcelSyncRequest
import com.kumanodormitory.pokke.data.remote.dto.ParcelSyncResponse
import com.kumanodormitory.pokke.data.remote.dto.RyoseiListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PokkeApiService {

    @POST("api/ryosei/{id}/call")
    suspend fun callRyosei(
        @Path("id") ryoseiId: String,
        @Body body: CallRequest
    ): Response<Unit>

    @POST("api/parcels/sync")
    suspend fun syncParcels(
        @Body body: ParcelSyncRequest
    ): Response<ParcelSyncResponse>

    @GET("api/ryosei")
    suspend fun getRyosei(
        @Query("updated_after") updatedAfter: String? = null
    ): Response<RyoseiListResponse>

    @GET("api/health")
    suspend fun health(): Response<Unit>
}
