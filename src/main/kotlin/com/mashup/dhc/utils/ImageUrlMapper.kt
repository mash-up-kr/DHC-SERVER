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

        fun getHobbyImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("hobby", format)

        fun getFriendImageUrl(format: ImageFormat = ImageFormat.SVG): Image = Image.forMissionCategory("friend", format)

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
}