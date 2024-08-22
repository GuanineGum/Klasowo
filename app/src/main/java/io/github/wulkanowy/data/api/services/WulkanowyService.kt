package io.github.guanine.data.api.services

import io.github.guanine.data.api.models.Mapping
import io.github.guanine.data.db.entities.AdminMessage
import retrofit2.http.GET
import javax.inject.Singleton

@Singleton
interface WulkanowyService {

    @GET("/v1.json")
    suspend fun getAdminMessages(): List<AdminMessage>

    @GET("/mapping4.json")
    suspend fun getMapping(): Mapping
}
