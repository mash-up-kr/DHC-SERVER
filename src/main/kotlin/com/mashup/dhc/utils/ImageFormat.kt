package com.mashup.dhc.utils

import kotlinx.serialization.Serializable

enum class ImageFormat(
    val extension: String
) {
    SVG("svg"),
    PNG("png")
}

@Serializable
@JvmInline
value class Image(
    private val url: String
) {
    companion object {
        private const val BASE_URL = "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/axircf8nexkb/b/dhc-storage/o"

        /**
         * 미션 카테고리 이미지 URL 생성
         * 폴더 구조: /logos/category/{format}/{category}.{extension}
         */
        fun forMissionCategory(
            category: String,
            format: ImageFormat = ImageFormat.SVG
        ): Image = Image("$BASE_URL/logos/category/${format.extension}/$category.${format.extension}")

        /**
         * 동물 카드 이미지 URL 생성
         * 폴더 구조: /logos/animal/{format}/{color}_{animal}.{extension}
         */
        fun forAnimalCard(
            color: String,
            animal: String,
            format: ImageFormat = ImageFormat.SVG
        ): Image = Image("$BASE_URL/logos/animal/${format.extension}/${color}_$animal.${format.extension}")

        /**
         * 운세 관련 이미지 URL 생성
         * 폴더 구조: /logos/fortune/{format}/{fortuneType}.{extension}
         */
        fun forFortune(
            fortuneType: String,
            format: ImageFormat = ImageFormat.SVG
        ): Image = Image("$BASE_URL/logos/fortune/${format.extension}/$fortuneType.${format.extension}")

        /**
         * 홈 화면 운세 이미지 URL 생성
         * 폴더 구조: /logos/mainCard/{format}/{fortuneType}.{extension}
         */
        fun forMainCard(
            fortuneType: String,
            format: ImageFormat = ImageFormat.SVG
        ): Image = Image("$BASE_URL/logos/mainCard/${format.extension}/$fortuneType.${format.extension}")

        /**
         * 빈 문자열의 경우 그대로 유지
         */
        fun custom(path: String): Image =
            when {
                path.isEmpty() -> Image("") // 빈 문자열 그대로 유지
                path.startsWith("http") -> Image(path)
                else -> Image("$BASE_URL/$path")
            }
    }

    override fun toString(): String = url
}