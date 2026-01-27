package com.mashup.dhc.routes

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

open class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)

class InternalServerErrorException(
    val errorCode: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR
) : RuntimeException(errorCode.message)

@Serializable
enum class ErrorCode(
    val code: Int,
    val message: String,
    val httpStatus: HttpStatusCode
) {
    // Client Errors - 1xxx
    INVALID_REQUEST(1001, "잘못된 요청입니다", HttpStatusCode.BadRequest),
    INVALID_JSON_FORMAT(1002, "잘못된 JSON 형식입니다", HttpStatusCode.BadRequest),
    MISSING_REQUIRED_FIELD(1003, "필수 항목이 누락되었습니다", HttpStatusCode.BadRequest),
    INVALID_FIELD_VALUE(1004, "잘못된 값입니다", HttpStatusCode.BadRequest),

    // Validation Errors - 1100
    VALIDATION_FAILED(1100, "입력값 검증에 실패했습니다", HttpStatusCode.BadRequest),
    EMPTY_USER_TOKEN(1101, "사용자 토큰을 입력해주세요", HttpStatusCode.BadRequest),
    NO_MISSION_CATEGORY_SELECTED(1103, "최소 하나의 미션 카테고리를 선택해주세요", HttpStatusCode.BadRequest),
    TOO_MANY_MISSION_CATEGORIES(1104, "미션 카테고리는 최대 6개까지 선택 가능합니다", HttpStatusCode.BadRequest),
    INVALID_BIRTH_DATE(1105, "생년월일은 미래 날짜일 수 없습니다", HttpStatusCode.BadRequest),
    INVALID_TIME_FORMAT(1108, "잘못된 시간 형식입니다", HttpStatusCode.BadRequest),
    FUTURE_MISSION_COMPLETION(1109, "미래 날짜의 미션은 완료할 수 없습니다", HttpStatusCode.BadRequest),
    OLD_MISSION_COMPLETION(1110, "30일 이전의 미션은 완료할 수 없습니다", HttpStatusCode.BadRequest),

    // Client Errors
    CONFLICT(3002, "Conflict", HttpStatusCode.Conflict),
    MAXIMUM_SWITCH_COUNT_EXCEEDED(
        3003,
        "Maximum switch count exceeded",
        HttpStatusCode.BadRequest
    ),

    // Business Logic Errors - 2xxx
    USER_ALREADY_EXISTS(2001, "이미 존재하는 사용자입니다", HttpStatusCode.Conflict),
    USER_NOT_FOUND(2002, "사용자를 찾을 수 없습니다", HttpStatusCode.NotFound),
    MISSION_NOT_FOUND(2003, "미션을 찾을 수 없습니다", HttpStatusCode.NotFound),
    SHARE_NOT_FOUND(2004, "공유 정보를 찾을 수 없습니다", HttpStatusCode.NotFound),
    LEVEL_NOT_ENOUGH(2005, "레벨이 부족합니다 (레벨 8 이상 필요)", HttpStatusCode.Forbidden),
    YEARLY_FORTUNE_ALREADY_USED(2006, "이미 1년 운세를 사용했습니다", HttpStatusCode.BadRequest),
    YEARLY_FORTUNE_NOT_CREATED(2007, "1년 운세가 아직 생성되지 않았습니다. 먼저 운세를 열어주세요", HttpStatusCode.NotFound),

    // Auth Errors - 3xxx
    UNAUTHORIZED(3001, "인증이 필요합니다", HttpStatusCode.Unauthorized),
    FORBIDDEN(3002, "접근 권한이 없습니다", HttpStatusCode.Forbidden),

    // Resource Errors - 4xxx
    NOT_FOUND(4001, "요청한 리소스를 찾을 수 없습니다", HttpStatusCode.NotFound),
    FILE_NOT_FOUND(4002, "파일을 찾을 수 없습니다", HttpStatusCode.NotFound),

    // Server Errors - 5xxx
    INTERNAL_SERVER_ERROR(5000, "서버 오류가 발생했습니다", HttpStatusCode.InternalServerError)
}

class ValidationException(
    val errors: List<ErrorCode>
) : BusinessException(ErrorCode.VALIDATION_FAILED) {
    val errorMessages: List<String> = errors.map { it.message }
}