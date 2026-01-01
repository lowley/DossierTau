package lorry.dossiertau.usecases.generateHTMLs.support

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface MoviesApi {
    @GET("all/search")
    suspend fun fetchPage(
        @Query("q") title: String,
    ): ResponseBody

    @GET("/2913914/her-limit-11-porn-video.html")
    suspend fun fetchPage2(
    ): ResponseBody
}

interface IpFindApi{
    @GET(".")
    suspend fun fetchPage(
    ): ResponseBody


}
