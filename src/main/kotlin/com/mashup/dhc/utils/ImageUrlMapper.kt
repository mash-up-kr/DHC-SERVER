package com.mashup.dhc.utils

import com.mashup.dhc.routes.ANIMAL
import com.mashup.dhc.routes.COLOR

object ImageUrlMapper {
    private const val BASE_URL = "https://kr.object.ncloudstorage.com/dhc-object-storage"


    /**
     * 미션 카테고리 이미지 URL
     */
    object MissionCategory {
        fun getTransportationImageUrl(): String = "$BASE_URL/logos/transportaion.png"

        fun getFoodImageUrl(): String = "$BASE_URL/logos/food.png"

        fun getDigitalImageUrl(): String = "$BASE_URL/logos/digital.png"

        fun getShoppingImageUrl(): String = "$BASE_URL/logos/shopping.png"

        fun getHobbyImageUrl(): String = "$BASE_URL/logos/hobby.png"

        fun getFriendImageUrl(): String = "$BASE_URL/logos/friend.png"
    }

    /**
     * 동물 카드 이미지 URL
     */
    fun getAnimalCardImageUrl(
        color: COLOR,
        animal: ANIMAL
    ): String = "$BASE_URL/logos/${color.englishName}_${animal.englishName}.svg"

    /**
     * 운세 상세 이미지 URL
     */
    object Fortune {
        fun getJinxedColorImageUrl(): String = "$BASE_URL/fortune-detail/jinxedColor.svg"

        fun getJinxedMenuImageUrl(): String = "$BASE_URL/fortune-detail/jinxedMenu.svg"

        fun getLuckyColorImageUrl(): String = "$BASE_URL/fortune-detail/luckyColor.svg"

        fun getTodayMenuImageUrl(): String = "$BASE_URL/fortune-detail/todayMenu.svg"
    }
}