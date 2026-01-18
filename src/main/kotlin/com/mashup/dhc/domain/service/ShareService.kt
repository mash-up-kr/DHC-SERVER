package com.mashup.dhc.domain.service

import com.mashup.dhc.domain.model.Share
import com.mashup.dhc.domain.model.ShareRepository
import com.mashup.dhc.domain.model.UserRepository
import com.mashup.dhc.routes.BusinessException
import com.mashup.dhc.routes.ErrorCode
import org.bson.types.ObjectId

class ShareService(
    private val shareRepository: ShareRepository,
    private val userRepository: UserRepository
) {
    suspend fun createShareCode(userId: String): ShareCodeResult {
        val userObjectId =
            try {
                ObjectId(userId)
            } catch (e: IllegalArgumentException) {
                throw BusinessException(ErrorCode.INVALID_REQUEST)
            }

        val user =
            userRepository.findById(userObjectId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val share = Share.create(user.id!!)
        shareRepository.insertOne(share)

        return ShareCodeResult(
            shareCode = share.shareCode
        )
    }

    suspend fun completeShare(shareCode: String): ShareCompleteResult {
        val share =
            shareRepository.findByShareCode(shareCode)
                ?: throw BusinessException(ErrorCode.SHARE_NOT_FOUND)

        if (share.completed) {
            return ShareCompleteResult(
                shareCode = shareCode,
                alreadyCompleted = true
            )
        }

        val modifiedCount = shareRepository.markAsCompleted(shareCode)

        return ShareCompleteResult(
            shareCode = shareCode,
            alreadyCompleted = modifiedCount == 0L
        )
    }

    suspend fun getShareByCode(shareCode: String): Share =
        shareRepository.findByShareCode(shareCode)
            ?: throw BusinessException(ErrorCode.SHARE_NOT_FOUND)
}

data class ShareCodeResult(
    val shareCode: String
)

data class ShareCompleteResult(
    val shareCode: String,
    val alreadyCompleted: Boolean
)