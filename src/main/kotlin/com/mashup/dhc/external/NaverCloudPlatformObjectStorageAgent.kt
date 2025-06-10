package com.mashup.dhc.external

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// https://sdk.amazonaws.com/kotlin/api/latest/s3/index.html
class NaverCloudPlatformObjectStorageAgent(
    private val accessKey: String,
    private val secretKey: String,
    private val bucketName: String
) {
    private val s3Client =
        S3Client {
            region = "kr-standard"
            endpointUrl = Url.parse("https://kr.object.ncloudstorage.com")
            credentialsProvider =
                StaticCredentialsProvider {
                    accessKeyId = accessKey
                    secretAccessKey = secretKey
                }
        }

    // 파일 업로드하고 URL 반환
    suspend fun upload(
        key: String,
        data: ByteArray,
        contentType: String? = null
    ): String {
        withContext(Dispatchers.IO) {
            val request =
                PutObjectRequest {
                    bucket = bucketName
                    this.key = key
                    body = ByteStream.fromBytes(data)
                    contentType?.let { this.contentType = it }
                    acl = ObjectCannedAcl.PublicRead
                }

            s3Client.putObject(request)
        }
        return getUrl(key)
    }

    suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            val objectId = ObjectIdentifier { this.key = key }
            val deleteRequest = Delete { objects = listOf(objectId) }
            val request =
                DeleteObjectsRequest {
                    bucket = bucketName
                    delete = deleteRequest
                }
            s3Client.deleteObjects(request)
        }
    }

    fun getUrl(key: String): String = "https://$bucketName.kr.object.ncloudstorage.com/$key"
}