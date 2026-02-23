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
    val subTitle: String,
    val animalName: String
) {
    SCORE_0_2(0..2, "덫에 걸린 날", "오늘은 지갑에서 도망치는게 좋겠어요.", "도망치는 쥐"),
    SCORE_3_6(3..6, "부패한 지갑", "슬픈 기운이 지갑까지 내려왔어요. 오늘은 조심하세요.", "슬픈 쥐"),
    SCORE_7_10(7..10, "초조한", "쥐처럼 주변을 살피며 새는 돈부터 체크해봐요.", "쥐"),
    SCORE_11_13(11..13, "공허한 날", "뭘해도 허무한 날이에요. 오늘은 힘을 아껴두세요.", "허무한 소"),
    SCORE_14_17(14..17, "무기력한 돈", "묵묵히 버티는 흐름이에요. 괜히 움직이면 피곤해져요.", "묵묵한 소"),
    SCORE_18_21(18..21, "먹이를 놓친 날", "기회가 스쳐가요. 욕심을 줄이면 아쉬움도 줄어요.", "아쉬운 호랑이"),
    SCORE_22_24(22..24, "조용한 날", "상황을 읽는 기운이에요. 움직이기보단 지켜보세요.", "조용한 호랑이"),
    SCORE_25_28(25..28, "불안한 금전운", "불안이 판단을 흐려요. 꼭 필요한 것만 챙겨요!", "겁먹은 토끼"),
    SCORE_29_32(29..32, "조심스러운 날", "적게 움직이고, 조심하면 손해는 피할 수 있어요.", "조용한 토끼"),
    SCORE_33_35(33..35, "날아오를 준비", "아직은 준비의 시간이에요. 정리한 만큼 기회가 쌓여요.", "준비하는 용"),
    SCORE_36_39(36..39, "기회를 보는 날", "기회는 천천히 올라와요. 지금은 흐름을 봐야해요.", "차분한 용"),
    SCORE_40_43(40..43, "조심해야 하는 날", "신중함이 필요한 날, 필요한 지출을 적어봐요.", "신중한 뱀"),
    SCORE_44_46(44..46, "조심해야 하는 날", "예민함이 지출로 이어져요. 감정과 결제를 분리해봐요.", "독 품은 뱀"),
    SCORE_47_50(47..50, "달릴 준비 하는 날", "금전운 상승 직전이에요. 큰 지출은 아직이에요.", "얌전한 말"),
    SCORE_51_54(51..54, "부지런한 날", "기운이 올라오는 날, 움직인 만큼 결과가 와요.", "설레는 말"),
    SCORE_55_57(55..57, "평화로운 날", "서두를 필요 없어요. 천천히 가도 손해는 없어요.", "걷는 양"),
    SCORE_58_61(58..61, "풍성한 날", "마음이 느슨해질 수 있어요. 작은 보상 정도는 괜찮아요.", "여유로운 양"),
    SCORE_62_65(62..65, "기대되는 날", "생각이 많아지는 날이에요. 큰 지출은 조금 미뤄봐요.", "설레는 원숭이"),
    SCORE_66_68(66..68, "뜻밖의 횡재", "재미와 기회가 함께 와요. 흥만 조심하면 좋아요.", "신난 원숭이"),
    SCORE_69_72(69..72, "빛이 스며드는 날", "판단이 또렷해지는 날, 결정할 게 있다면 좋아요.", "설레는 닭"),
    SCORE_73_76(73..76, "빛나는 흐름", "선택과 결과가 잘 맞아요. 정리하기에 좋은 날이에요.", "행복한 닭"),
    SCORE_77_80(77..80, "행운의 떨림", "금전 흐름이 좋아요. 기준 안에서 지출은 좋아요.", "신난 강아지"),
    SCORE_81_84(81..84, "행운이 머무는 날", "안정적인 흐름이에요. 익숙한 선택이 가장 편해요.", "행복한 강아지"),
    SCORE_85_88(85..88, "최고의 날", "방향만 맞다면, 지출 및 투자가 성과로 돌아와요.", "달리는 강아지"),
    SCORE_89_92(89..92, "행운의 날", "풍요로운 기운이 들어와요. 기회가 오면 잡아도 돼요!", "신난 돼지"),
    SCORE_93_96(93..96, "최고의 날", "배부른 하루가 되겠어요. 여기저기서 돈이 넘쳐나요.", "배부른 돼지"),
    SCORE_97_100(97..100, "최고 행운의 날", "금전운 최고의 날! 손대는 것마다 돈이 붙어요.", "황금 돼지");

    fun getCardImage(format: ImageFormat = ImageFormat.PNG): Image =
        Image.forMainCard("${range.first}-${range.last}", format)

    companion object {
        fun fromScore(score: Int): FortuneScoreRange =
            entries.find { score in it.range } ?: SCORE_47_50
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
        fortuneTitle = scoreRange.subTitle,
        fortuneDetail = fortuneDetail,
        totalScore = totalScore,
        tips = this.toTips(),
        cardInfo =
            FortuneCard(
                image = scoreRange.getCardImage(),
                title = scoreRange.animalName,
                subTitle = scoreRange.title
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

// Gemini 궁합 테스트 응답 모델
@Serializable
data class LoveTestGeminiResponse(
    val score: Int,
    @SerialName("fortune_detail") val fortuneDetail: String,
    @SerialName("lucky_color") val luckyColor: String,
    @SerialName("lucky_color_hex") val luckyColorHex: String,
    @SerialName("jinxed_color") val jinxedColor: String,
    @SerialName("jinxed_color_hex") val jinxedColorHex: String,
    @SerialName("today_menu") val todayMenu: String,
    @SerialName("jinxed_menu") val jinxedMenu: String,
    @SerialName("confess_date") val confessDate: String,
    @SerialName("confess_location") val confessLocation: String
)