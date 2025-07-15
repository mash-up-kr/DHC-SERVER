package com.mashup.dhc.utils

import com.mashup.dhc.routes.ANIMAL
import com.mashup.dhc.routes.COLOR

object ImageUrlMapper {
    /**
     * 미션 카테고리 이미지 URL
     */
    object MissionCategory {
        fun getTransportationImageUrl(format: ImageFormat = ImageFormat.SVG): Image =
            Image.forMissionCategory("transportation", format)

        fun getFoodImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("food", format)

        fun getDigitalImageUrl(format: ImageFormat = ImageFormat.SVG): Image =
            Image.forMissionCategory("digital", format)

        fun getShoppingImageUrl(format: ImageFormat = ImageFormat.SVG): Image =
            Image.forMissionCategory("shopping", format)

        fun getTravelImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("travel", format)

        fun getSocialImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("social", format)

        fun getReflectionImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("", format)
    }

    /**
     * 동물 카드 이미지 URL
     */
    fun getAnimalCardImageUrl(
        color: COLOR,
        animal: ANIMAL,
        format: ImageFormat = ImageFormat.SVG
    ): Image = Image.forAnimalCard(color.englishName, animal.englishName, format)

    /**
     * 운세 상세 이미지 URL
     */
    object Fortune {
        fun getJinxedColorImageUrl(format: ImageFormat = ImageFormat.SVG): Image =
            Image.forFortune("jinxedColor", format)

        fun getJinxedMenuImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("jinxedMenu", format)

        fun getLuckyColorImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("luckyColor", format)

        fun getTodayMenuImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("todayMenu", format)
    }

    /**
     * 홈 화면 운세 이미지 URL
     */
    object MainCard {
        fun getFortuneCardByScore(
            totalScore: Int,
            format: ImageFormat = ImageFormat.PNG
        ): Image {
            val scoreRange =
                when (totalScore) {
                    in 0..2 -> "0-2"
                    in 3..6 -> "3-6"
                    in 7..10 -> "7-10"
                    in 11..13 -> "11-13"
                    in 14..17 -> "14-17"
                    in 18..21 -> "18-21"
                    in 22..24 -> "22-24"
                    in 25..28 -> "25-28"
                    in 29..32 -> "29-32"
                    in 33..35 -> "33-35"
                    in 36..39 -> "36-39"
                    in 40..43 -> "40-43"
                    in 44..46 -> "44-46"
                    in 47..50 -> "47-50"
                    in 51..54 -> "51-54"
                    in 55..57 -> "55-57"
                    in 58..61 -> "58-61"
                    in 62..65 -> "62-65"
                    in 66..68 -> "66-68"
                    in 69..72 -> "69-72"
                    in 73..76 -> "73-76"
                    in 77..80 -> "77-80"
                    in 81..84 -> "81-84"
                    in 85..88 -> "85-88"
                    in 89..92 -> "89-92"
                    in 93..96 -> "93-96"
                    in 97..100 -> "97-100"
                    else -> {
                        ""
                    }
                }
            return Image.forMainCard(scoreRange, format)
        }
    }
}