package com.mashup.dhc.domain.model

import com.mashup.dhc.domain.model.UserRepository.Companion.USER_COLLECTION
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

// MongoDB 캐시 모델
data class MonthlyFortune(
    @BsonId val id: String = ObjectId().toString(),
    val month: Int,
    val year: Int,
    val dailyFortuneList: List<DailyFortune>, // Gemini 응답 저장
    val createdAt: Long = System.currentTimeMillis()
)

class FortuneRepository(
    private val database: MongoDatabase
) {
    suspend fun pushMonthlyFortune(
        userId: String,
        monthlyFortune: MonthlyFortune
    ) {
        database
            .getCollection<User>(USER_COLLECTION)
            .updateOne(
                Filters.eq("_id", userId),
                Updates.push(User::monthlyFortuneList.name, monthlyFortune)
            )
    }
}

// 일일 운세 객체
@Serializable
data class DailyFortune(
    val date: String,
    @SerialName("fortune_title") val fortuneTitle: String,
    @SerialName("fortune_detail") val fortuneDetail: String,
    @SerialName("jinxed_color") val jinxedColor: String,
    @SerialName("jinxed_color_hex") val jinxedColorHex: String,
    @SerialName("jinxed_menu") val jinxedMenu: String,
    @SerialName("jinxed_number") val jinxedNumber: Int,
    @SerialName("lucky_color") val luckyColor: String,
    @SerialName("lucky_color_hex") val luckyColorHex: String,
    @SerialName("lucky_number") val luckyNumber: Int,
    val score: Int,
    @SerialName("today_menu") val todayMenu: String
)