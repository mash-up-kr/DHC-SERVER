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
            entries.find { it.description == description }
                ?: throw IllegalArgumentException("$description is not a valid color")
    }
}

enum class FortuneScoreRange(
    val range: IntRange,
    val title: String,
    val subTitle: String
) {
    SCORE_0_2(0..2, "금전운 최악의 날", "깨진 그릇"),
    SCORE_3_6(3..6, "금전운 최악의 날", "깨진 그릇"),
    SCORE_7_10(7..10, "얼어붙은 흐름", "얼음"),
    SCORE_11_13(11..13, "공허한 날", "빈 지갑"),
    SCORE_14_17(14..17, "조각난 하루", "깨진 은화"),
    SCORE_18_21(18..21, "돈이 휩쓸리는날", "소용돌이"),
    SCORE_22_24(22..24, "지갑 주의보", "부서진 자물쇠"),
    SCORE_25_28(25..28, "먹구름이 낀 날", "비구름"),
    SCORE_29_32(29..32, "대비가 필요한 날", "우산"),
    SCORE_33_35(33..35, "예상치 못한 충격", "번개"),
    SCORE_36_39(36..39, "빛이 흩어지는 날", "부서진 보석"),
    SCORE_40_43(40..43, "예상치 못한 행운", "별똥별"),
    SCORE_44_46(44..46, "길을 잃은날", "나침반"),
    SCORE_47_50(47..50, "조심해~", "흘러간 시간"),
    SCORE_51_54(51..54, "무리없는 날", "중이 비행기"),
    SCORE_55_57(55..57, "하나하나 차근차근", "새싹"),
    SCORE_58_61(58..61, "좋은 일이 기대되는", "황금"),
    SCORE_62_65(62..65, "기회가 눈앞에!", "보검"),
    SCORE_66_68(66..68, "한줄기의", "빛"),
    SCORE_69_72(69..72, "잔잔한 하루", "종"),
    SCORE_73_76(73..76, "마음이 말랑해지는 날", "무지개"),
    SCORE_77_80(77..80, "빛이 스며드는 날", "보석"),
    SCORE_81_84(81..84, "빛나는 흐름", "다이아몬드"),
    SCORE_85_88(85..88, "행운의 떨림", "하트"),
    SCORE_89_92(89..92, "행운이 머무는 날", "네잎클로버"),
    SCORE_93_96(93..96, "금전운 최고의 날", "왕관"),
    SCORE_97_100(97..100, "금전운 최고의 날", "왕관");

    companion object {
        fun fromScore(score: Int): FortuneScoreRange = entries.find { score in it.range } ?: SCORE_47_50
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

fun DailyFortune.toResponse(): DailyFortuneResponse {
    val scoreRange = FortuneScoreRange.fromScore(totalScore)

    return DailyFortuneResponse(
        date = date,
        fortuneTitle = fortuneTitle,
        fortuneDetail = fortuneDetail,
        totalScore = totalScore,
        tips = this.toTips(),
        cardInfo =
            FortuneCard(
                image = ImageUrlMapper.MainCard.getFortuneCardByScore(totalScore, ImageFormat.PNG),
                title = scoreRange.title,
                subTitle = scoreRange.subTitle
            ),
        positiveScore = positiveScore,
        negativeScore = negativeScore
    )
}

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