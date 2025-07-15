package com.mashup.dhc.domain.model

import com.mashup.dhc.domain.model.UserRepository.Companion.USER_COLLECTION
import com.mashup.dhc.utils.Image
import com.mashup.dhc.utils.ImageFormat
import com.mashup.dhc.utils.ImageUrlMapper
import com.mongodb.MongoException
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonValue
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun insertDailyOne(dailyFortune: DailyFortune): BsonValue? {
        try {
            val result =
                database
                    .getCollection<DailyFortune>(FORTUNE_COLLECTION)
                    .insertOne(dailyFortune)
            return result.insertedId
        } catch (e: MongoException) {
            System.err.println("Unable to insert due to an error: $e")
        }
        return null
    }

    suspend fun retrieveArbitraryDailyFortune(): DailyFortune? =
        database
            .getCollection<DailyFortune>(FORTUNE_COLLECTION)
            .aggregate<DailyFortune>(listOf(Aggregates.sample(1)))
            .firstOrNull()

    suspend fun upsertMonthlyFortune(
        userId: String,
        monthlyFortune: MonthlyFortune
    ) {
        try {
            val filter = Filters.eq("_id", ObjectId(userId))

            val result =
                database
                    .getCollection<User>(USER_COLLECTION)
                    .updateOne(
                        filter = filter,
                        update = set(User::dailyFortunes.name, monthlyFortune.dailyFortuneList),
                        options = UpdateOptions().upsert(false)
                    )

            if (result.matchedCount == 0L) {
                throw IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun upsertDailyFortunes(
        userId: String,
        dailyFortunes: List<DailyFortune>
    ) {
        try {
            val filter = Filters.eq("_id", ObjectId(userId))

            val result =
                database
                    .getCollection<User>(USER_COLLECTION)
                    .updateOne(
                        filter = filter,
                        update = set(User::dailyFortunes.name, dailyFortunes),
                        options = UpdateOptions().upsert(false)
                    )

            if (result.matchedCount == 0L) {
                throw IllegalArgumentException("사용자를 찾을 수 없습니다: $userId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    companion object {
        const val FORTUNE_COLLECTION = "fortune"
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
    @SerialName("positive_score") val positiveScore: Int,
    @SerialName("negative_score") val negativeScore: Int,
    @SerialName("today_menu") val todayMenu: String
) {
    // 총점 계산 (직렬화에 포함됨)
    @SerialName("total_score")
    val totalScore: Int
        get() = 50 + (positiveScore - negativeScore) / 2
    val luckyColorType: FortuneColor
        get() = FortuneColor.fromKor(luckyColor)
    val jinxedColorType: FortuneColor
        get() = FortuneColor.fromKor(jinxedColor)
}

enum class FortuneColor(
    val description: String
) {
    GREEN("초록색"),
    RED("빨간색"),
    YELLOW("노란색"),
    WHITE("흰색"),
    BLACK("검정색");

    companion object {
        fun fromKor(description: String): FortuneColor =
            values().find { it.description == description }
                ?: throw IllegalArgumentException("$description is not a valid color")
    }
}

@Serializable
data class DailyFortuneResponse(
    val date: String,
    val fortuneTitle: String,
    val fortuneDetail: String,
    val totalScore: Int,
    val positiveScore: Int,
    val negativeScore: Int,
    val tips: List<FortuneTip>,
    val cardInfo: FortuneCard
)

fun DailyFortune.toTips(format: ImageFormat = ImageFormat.SVG): List<FortuneTip> {
    val jinxedColor =
        FortuneTip(
            image = ImageUrlMapper.Fortune.getJinxedColorImageUrl(format),
            title = "피해야 할 색상",
            description = jinxedColor,
            hexColor = jinxedColorHex
        )

    val jinxedMenu =
        FortuneTip(
            image = ImageUrlMapper.Fortune.getJinxedMenuImageUrl(format),
            title = "피해야 할 음식",
            description = jinxedMenu
        )

    val todayMenu =
        FortuneTip(
            image = ImageUrlMapper.Fortune.getTodayMenuImageUrl(format),
            title = "오늘의 추천메뉴",
            description = todayMenu
        )

    val luckyColor =
        FortuneTip(
            image = ImageUrlMapper.Fortune.getLuckyColorImageUrl(format),
            title = "행운의 색상",
            description = luckyColor,
            hexColor = luckyColorHex
        )

    val tips = listOf(todayMenu, luckyColor, jinxedColor, jinxedMenu)
    return tips
}

fun DailyFortune.toResponse(): DailyFortuneResponse =
    DailyFortuneResponse(
        date = date,
        fortuneTitle = fortuneTitle,
        fortuneDetail = fortuneDetail,
        totalScore = totalScore,
        tips = this.toTips(),
        cardInfo =
            FortuneCard(
                image =
                    Image.custom(
                        "https://kr.object.ncloudstorage.com/dhc-object-storage/logos/mainCard/png/fourLeafClover.png"
                    ),
                title = "최고의 날",
                subTitle = "네잎클로버"
            ),
        positiveScore = positiveScore,
        negativeScore = negativeScore
    )

@Serializable
data class FortuneTip(
    val image: Image,
    val title: String,
    val description: String,
    val hexColor: String? = null
)

@Serializable
data class FortuneCard(
    val image: Image,
    val title: String,
    val subTitle: String
)