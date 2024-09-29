package com.travis.inshape

import retrofit2.Response
import retrofit2.http.GET

interface NutritionApi {
    @GET("/v3/b/66eecbbae41b4d34e4346405")
    suspend fun getNutritionData(): Response<NutritionResponse>
}