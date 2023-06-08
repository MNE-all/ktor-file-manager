package com.fmanager.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fmanager.utils.JWTPrefs
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*



fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain

    authentication {
        jwt {
            with(JWTPrefs){
                realm = jwtRealm
                    verifier(
                        JWT
                            .require(Algorithm.HMAC256(jwtSecret))
                            .withAudience(jwtAudience)
                            .withIssuer(jwtDomain)
                            .build()
                    )
                validate { credential ->
                    if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
                }
                challenge { defaultScheme, realm ->
                    call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
                }
            }

        }
    }
}
