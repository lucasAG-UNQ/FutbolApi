package com.grupob.futbolapi.aspects

import com.grupob.futbolapi.model.Request
import com.grupob.futbolapi.repositories.RequestRepository
import com.grupob.futbolapi.repositories.UserRepository
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime

@Aspect
@Component
class RequestLoggingAspect(
    private val userRepository: UserRepository,
    private val requestRepository: RequestRepository
) {

    @Pointcut("execution(public * com.grupob.futbolapi.webServices.TeamController.*(..))")
    fun teamControllerMethods() {
        // This is a pointcut definition, so it intentionally has no body.
    }

    @Before("teamControllerMethods()")
    fun logRequest(joinPoint: JoinPoint) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.principal != "anonymousUser") {
            val username = authentication.name
            val user = userRepository.findByUsername(username)

            if (user != null) {
                val requestAttributes = RequestContextHolder.currentRequestAttributes() as? ServletRequestAttributes
                val requestUri = requestAttributes?.request?.requestURI ?: "unknown"

                val request = Request(
                    endpoint = requestUri,
                    timestamp = LocalDateTime.now(),
                    user = user
                )
                requestRepository.save(request)
            }
        }
    }
}
