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

        fun getOverviewMoneyImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("money", format)

        fun getOverviewLoveImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("love", format)

        fun getOverviewStudyImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forFortune("study", format)
    }

    /**
     * 리워드 레벨 이미지 URL
     */
    fun getRewardLevelImageUrl(level: Int): Image {
        // 이미지는 레벨 8까지만 존재하므로, 8 이상은 8레벨 이미지를 사용
        val targetLevel = if (level > 8) 8 else level
        return Image.forReward(targetLevel)
    }
}