package com.blogmd.mizukiwriter.data.deployment

import com.blogmd.mizukiwriter.data.github.githubJson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private interface VercelApi {
    @GET("v9/projects/{projectName}")
    suspend fun getProject(
        @Path("projectName") projectName: String,
        @Query("teamId") teamId: String? = null,
    ): VercelProject

    @POST("v10/projects")
    suspend fun createProject(
        @Body request: VercelCreateProjectRequest,
        @Query("teamId") teamId: String? = null,
    ): VercelProject

    @POST("v10/projects/{projectName}/domains")
    suspend fun addDomain(
        @Path("projectName") projectName: String,
        @Body request: VercelAddDomainRequest,
        @Query("teamId") teamId: String? = null,
    ): VercelDomainResponse
}

private interface CloudflarePagesApi {
    @GET("accounts/{accountId}/pages/projects/{projectName}")
    suspend fun getProject(
        @Path("accountId") accountId: String,
        @Path("projectName") projectName: String,
    ): CloudflareEnvelope<CloudflareProject>

    @POST("accounts/{accountId}/pages/projects")
    suspend fun createProject(
        @Path("accountId") accountId: String,
        @Body request: CloudflareCreateProjectRequest,
    ): CloudflareEnvelope<CloudflareProject>

    @POST("accounts/{accountId}/pages/projects/{projectName}/domains")
    suspend fun addDomain(
        @Path("accountId") accountId: String,
        @Path("projectName") projectName: String,
        @Body request: CloudflareDomainRequest,
    ): CloudflareEnvelope<CloudflareDomainResult>
}

fun createVercelGateway(token: String): VercelGateway {
    val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.vercel.com/")
        .client(okHttp)
        .addConverterFactory(githubJson.asConverterFactory("application/json".toMediaType()))
        .build()
    val api = retrofit.create(VercelApi::class.java)
    return object : VercelGateway {
        override suspend fun getProject(projectName: String, teamId: String?): VercelProject =
            api.getProject(projectName, teamId)

        override suspend fun createProject(request: VercelCreateProjectRequest, teamId: String?): VercelProject =
            api.createProject(request, teamId)

        override suspend fun addDomain(
            projectName: String,
            request: VercelAddDomainRequest,
            teamId: String?,
        ): VercelDomainResponse = api.addDomain(projectName, request, teamId)
    }
}

fun createCloudflarePagesGateway(token: String): CloudflarePagesGateway {
    val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .client(okHttp)
        .addConverterFactory(githubJson.asConverterFactory("application/json".toMediaType()))
        .build()
    val api = retrofit.create(CloudflarePagesApi::class.java)
    return object : CloudflarePagesGateway {
        override suspend fun getProject(accountId: String, projectName: String): CloudflareEnvelope<CloudflareProject> =
            api.getProject(accountId, projectName)

        override suspend fun createProject(
            accountId: String,
            request: CloudflareCreateProjectRequest,
        ): CloudflareEnvelope<CloudflareProject> = api.createProject(accountId, request)

        override suspend fun addDomain(
            accountId: String,
            projectName: String,
            request: CloudflareDomainRequest,
        ): CloudflareEnvelope<CloudflareDomainResult> = api.addDomain(accountId, projectName, request)
    }
}
