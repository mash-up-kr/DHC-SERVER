package com.mashup.dhc.routes

import com.mashup.com.mashup.dhc.domain.service.UserService
import com.mashup.com.mashup.dhc.utils.BirthDate
import com.mashup.dhc.domain.model.Gender
import com.mashup.dhc.domain.model.MissionCategory
import com.mashup.dhc.utils.BirthTime
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

fun Route.register(userService: UserService) {
    route("/register") {
        post {
            val request = call.receive<RegisterUserRequest>()

            val registeredUserId =
                userService.registerUser(
                    request.userToken,
                    request.gender,
                    request.birthDate,
                    request.birthTime,
                    request.preferredMissionCategoryList
                )
            if (registeredUserId != null) {
                call.respond(RegisterUserResponse(registeredUserId.toString()))
            } else {
                call.respondText("Failed to register user", status = io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }
    }
}

@Serializable
class RegisterUserRequest(
    val userToken: String,
    val gender: Gender,
    val birthDate: BirthDate,
    val birthTime: BirthTime?,
    val preferredMissionCategoryList: List<MissionCategory>
)

@Serializable
class RegisterUserResponse(
    val id: String
)