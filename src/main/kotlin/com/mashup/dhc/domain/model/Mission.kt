package com.mashup.dhc.domain.model

import com.mashup.dhc.utils.Image
import com.mashup.dhc.utils.ImageFormat
import com.mashup.dhc.utils.ImageUrlMapper
import com.mashup.dhc.utils.Money
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.math.BigDecimal
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDate
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Mission(
    @BsonId val id: ObjectId?,
    val category: MissionCategory,
    val difficulty: Int,
    val type: MissionType,
    val finished: Boolean = false,
    val title: String,
    val cost: Money,
    val switchCount: Int = 0,
    val endDate: LocalDate?
)

enum class MissionCategory(
    val displayName: String
) {
    TRANSPORTATION("이동·교통"),
    FOOD("식음료"),
    DIGITAL("디지털·구독"),
    SHOPPING("쇼핑"),
    TRAVEL("취미·문화"),
    SOCIAL("사교·모임"),
    SELF_REFLECTION("회고");

    fun imageUrl(format: ImageFormat = ImageFormat.SVG): Image =
        when (this) {
            TRANSPORTATION -> ImageUrlMapper.MissionCategory.getTransportationImageUrl(format)
            FOOD -> ImageUrlMapper.MissionCategory.getFoodImageUrl(format)
            DIGITAL -> ImageUrlMapper.MissionCategory.getDigitalImageUrl(format)
            SHOPPING -> ImageUrlMapper.MissionCategory.getShoppingImageUrl(format)
            TRAVEL -> ImageUrlMapper.MissionCategory.getHobbyImageUrl(format)
            SOCIAL -> ImageUrlMapper.MissionCategory.getFriendImageUrl(format)
            SELF_REFLECTION -> Image.custom("")
        }
}

enum class MissionType {
    LONG_TERM,
    DAILY
}

class MissionRepository(
    private val mongoDatabase: MongoDatabase
) {
    suspend fun findById(
        missionId: ObjectId,
        session: ClientSession
    ): Mission? =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(session, org.bson.Document("_id", missionId))
            .firstOrNull()

    suspend fun findLongTermByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.LONG_TERM)))
            .toList()

    suspend fun findLongTermByCategory(
        category: MissionCategory,
        session: ClientSession
    ): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(session, and(eq("category", category), eq("type", MissionType.LONG_TERM)))
            .toList()

    suspend fun findDailyByCategory(category: MissionCategory): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(and(eq("category", category), eq("type", MissionType.DAILY)))
            .toList()

    suspend fun findByCategory(
        type: MissionType,
        category: MissionCategory,
        session: ClientSession
    ): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find(
                session,
                and(
                    or(eq("category", category), eq("category", MissionCategory.SELF_REFLECTION)),
                    eq("type", type)
                )
            ).toList()

    suspend fun findAll(): List<Mission> =
        mongoDatabase
            .getCollection<Mission>(MISSION_COLLECTION)
            .find()
            .toList()

    companion object {
        const val MISSION_COLLECTION = "mission"
    }
}

fun List<Mission>.calculateSavedMoney() =
    this
        .filter { it.finished }
        .map { it.cost }
        .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)

fun List<Mission>.calculateSpendMoney() =
    this
        .filterNot { it.finished }
        .map { it.cost }
        .reduceOrNull(Money::plus) ?: Money(BigDecimal.ZERO)