package com.mashup.dhc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YearlyFortune(
    val year: Int,
    @SerialName("generated_date") val generatedDate: String,
    @SerialName("total_score") val totalScore: Int,
    @SerialName("summary_title") val summaryTitle: String,
    @SerialName("summary_detail") val summaryDetail: String,
    @SerialName("fortune_overview") val fortuneOverview: FortuneOverview,
    @SerialName("five_elements") val fiveElements: FiveElements,
    @SerialName("yearly_energy_title") val yearlyEnergyTitle: String,
    @SerialName("yearly_energy_detail") val yearlyEnergyDetail: String,
    @SerialName("lucky_menu") val luckyMenu: String,
    @SerialName("lucky_color") val luckyColor: String,
    @SerialName("lucky_color_hex") val luckyColorHex: String,
    @SerialName("unlucky_menu") val unluckyMenu: String,
    @SerialName("unlucky_color") val unluckyColor: String,
    @SerialName("unlucky_color_hex") val unluckyColorHex: String
)

@Serializable
data class FortuneOverview(
    val money: FortuneCategory,
    val love: FortuneCategory,
    val study: FortuneCategory
)

@Serializable
data class FortuneCategory(
    val title: String,
    val description: String
)

@Serializable
data class FiveElements(
    @SerialName("dominant_element") val dominantElement: String,
    @SerialName("dominant_warning") val dominantWarning: String,
    val wood: ElementBalance,
    val fire: ElementBalance,
    val earth: ElementBalance,
    val metal: ElementBalance,
    val water: ElementBalance
)

@Serializable
data class ElementBalance(
    val percentage: Int,
    val status: ElementStatus
)

@Serializable
enum class ElementStatus {
    BALANCED,
    EXCESS,
    DEFICIENT
}