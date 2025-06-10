package com.mashup.dhc.domain.service

import com.mashup.com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.domain.model.DailyFortune
import com.mashup.dhc.domain.model.DayOfWeekCharacter
import com.mashup.dhc.domain.model.FortuneResult
import com.mashup.dhc.domain.model.FortuneScoreRange
import com.mashup.dhc.domain.model.GeminiDailyFortune

class FortuneHeaderBodyService {

    /**
     * Gemini API 응답을 클라이언트 응답으로 변환 (header, body 추가)
     */
    fun convertToClientResponse(
        geminiDailyFortune: List<GeminiDailyFortune>,
        birthDate: BirthDate,
        month: Int,
        year: Int
    ): FortuneResult {
        val date = birthDate.date
        val birthDayOfWeek = date.dayOfWeek

        val dailyFortunesWithHeaderBody = geminiDailyFortune.map {
            val (header, body) = generateHeaderAndBody(
                score = it.score,
                birthDayOfWeek = birthDayOfWeek,
                luckyColor = it.luckyColor,
                luckyNumber = it.luckyNumber,
                jinxedColor = it.jinxedColor,
                jinxedNumber = it.jinxedNumber,
                todayMenu = it.todayMenu,
                jinxedMenu = it.jinxedMenu
            )

            DailyFortune(
                header = header,
                body = body,
                date = it.date,
                jinxedColor = it.jinxedColor,
                jinxedColorHex = it.jinxedColorHex,
                jinxedMenu = it.jinxedMenu,
                jinxedNumber = it.jinxedNumber,
                luckyColor = it.luckyColor,
                luckyColorHex = it.luckyColorHex,
                luckyNumber = it.luckyNumber,
                score = it.score,
                todayMenu = it.todayMenu
            )
        }

        return FortuneResult(
            month = month,
            year = year,
            fortune = dailyFortunesWithHeaderBody
        )
    }

    /**
     * 점수와 생년월일 요일에 따라 header와 body 생성
     */
    private fun generateHeaderAndBody(
        score: Int,
        birthDayOfWeek: java.time.DayOfWeek,
        luckyColor: String,
        luckyNumber: Int,
        jinxedColor: String,
        jinxedNumber: Int,
        todayMenu: String,
        jinxedMenu: String
    ): Pair<String, String> {

        val scoreRange = FortuneScoreRange.values().find { score in it.range }
            ?: FortuneScoreRange.NORMAL

        val dayCharacter = DayOfWeekCharacter.values().find { it.dayOfWeek == birthDayOfWeek }
            ?: DayOfWeekCharacter.SUNDAY

        val header = generateHeader(scoreRange, dayCharacter)
        val body = generateBody(
            scoreRange = scoreRange,
            dayCharacter = dayCharacter,
            luckyColor = luckyColor,
            luckyNumber = luckyNumber,
            jinxedColor = jinxedColor,
            jinxedNumber = jinxedNumber,
            todayMenu = todayMenu,
            jinxedMenu = jinxedMenu
        )

        return Pair(header, body)
    }

    /**
     * 점수 구간과 요일 특성에 따른 header 생성
     */
    private fun generateHeader(scoreRange: FortuneScoreRange, dayCharacter: DayOfWeekCharacter): String {
        return when (scoreRange) {
            FortuneScoreRange.VERY_GOOD -> "${dayCharacter.character}으로 빛나는 최고의 금전운!"
            FortuneScoreRange.GOOD -> "${dayCharacter.description} 좋은 금전운의 날"
            FortuneScoreRange.NORMAL -> "${dayCharacter.character}이 필요한 평범한 금전운"
            FortuneScoreRange.BAD -> "${dayCharacter.description} 주의가 필요한 날"
            FortuneScoreRange.VERY_BAD -> "극도의 ${dayCharacter.character}이 필요한 금전운 주의일"
        }
    }

    /**
     * 상세한 body 메시지 생성
     */
    private fun generateBody(
        scoreRange: FortuneScoreRange,
        dayCharacter: DayOfWeekCharacter,
        luckyColor: String,
        luckyNumber: Int,
        jinxedColor: String,
        jinxedNumber: Int,
        todayMenu: String,
        jinxedMenu: String
    ): String {
        val baseAdvice = when (scoreRange) {
            FortuneScoreRange.VERY_GOOD -> "오늘은 ${dayCharacter.description} 최고의 날입니다. 투자나 새로운 금융 계획을 세우기에 적극 추천하는 날입니다."
            FortuneScoreRange.GOOD -> "오늘은 ${dayCharacter.description} 긍정적인 날입니다. 계획했던 금융 활동을 실행에 옮기기 좋은 시기입니다."
            FortuneScoreRange.NORMAL -> "오늘은 ${dayCharacter.description} 보통의 날입니다. 기본적인 가계 관리에 집중하며 안정적으로 지내세요."
            FortuneScoreRange.BAD -> "오늘은 ${dayCharacter.description} 신중함이 필요한 날입니다. 큰 지출이나 투자는 피하고 절약에 집중하세요."
            FortuneScoreRange.VERY_BAD -> "오늘은 ${dayCharacter.description} 극도로 조심스러운 날입니다. 모든 금전 관련 결정을 미루고 현 상황 유지에 집중하세요."
        }

        val luckyAdvice = "행운의 숫자 $luckyNumber, 행운의 색상은 ${luckyColor}입니다."
        val jinxedAdvice = "${jinxedNumber}와 ${jinxedColor}은 피하세요."
        val menuAdvice = "추천 메뉴는 $todayMenu, 피해야 할 음식은 ${jinxedMenu}입니다."

        return "$baseAdvice $luckyAdvice $jinxedAdvice $menuAdvice"
    }
} 