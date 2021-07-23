package com.example.mapapp

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationService {
    @GET("/maps/api/place/nearbysearch/json")
    suspend fun getShopInfo(
            @Query("key") apiKey: String,
            @Query("location") latlng: String,
            @Query("radius") radius: Int,
            @Query("type") type: String,
    ): Response<LocationResponse>

    @GET("/maps/api/place/nearbysearch/json")
    suspend fun getNextShopInfo(
            @Query("pagetoken") pagetoken: String,
            @Query("key") apiKey: String,
    ): Response<LocationResponse>
}

data class LocationResponse(
        val results: List<Shop>,
        val next_page_token: String,
)

data class Shop(
        val name: String,
        val geometry: LocationInfo,
)

data class LocationInfo(
        val location: LatLngInfo
)

data class LatLngInfo(
        val lat: Double,
        val lng: Double,
)