package com.blogmd.mizukiwriter.data.github

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.HTTP

interface GitHubContentApi {
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String,
    ): GitHubContentResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: GitHubContentRequest,
    ): GitHubContentResponse

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: GitHubDeleteRequest,
    ): GitHubContentResponse

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String,
    ): GitHubRefResponse

    @GET("repos/{owner}/{repo}/git/commits/{commitSha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("commitSha") commitSha: String,
    ): GitHubCommitResponse

    @GET("repos/{owner}/{repo}/git/trees/{treeSha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("treeSha") treeSha: String,
        @Query("recursive") recursive: Int = 1,
    ): GitHubTreeResponse

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitHubBlobRequest,
    ): GitHubBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitHubCreateTreeRequest,
    ): GitHubTreeResponse

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitHubCreateCommitRequest,
    ): GitHubCommitResponse

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String,
        @Body body: GitHubUpdateRefRequest,
    ): GitHubRefResponse
}
