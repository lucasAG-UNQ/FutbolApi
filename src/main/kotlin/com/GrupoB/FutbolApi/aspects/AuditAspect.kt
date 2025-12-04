package com.grupob.futbolapi.aspects

import org.apache.logging.log4j.LogManager
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Arrays

@Aspect
@Component
class AuditAspect {

    private val auditLogger = LogManager.getLogger(AuditAspect::class.java)

    @Pointcut("within(com.grupob.futbolapi.webServices..*)")
    fun webServiceMethods() {}
    /*
     * This pointcut definition is intentionally empty. It serves as a marker for the @Around advice
     * to apply to all methods within the `com.grupob.futbolapi.webServices` package and its subpackages.
     */

    @Around("webServiceMethods()")
    fun auditWebServiceCall(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        val authentication = SecurityContextHolder.getContext().authentication
        val user = if (authentication != null && authentication.isAuthenticated) authentication.name else "anonymous"

        val operation = joinPoint.signature.toShortString()
        val parameters = Arrays.toString(joinPoint.args)

        try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime
            val logMessage = "Timestamp: $timestamp | User: $user | Operation: $operation | Parameters: $parameters | Execution Time: ${executionTime}ms"
            auditLogger.info(logMessage)
            return result
        } catch (e: Throwable) {
            val executionTime = System.currentTimeMillis() - startTime
            val logMessage = "Timestamp: $timestamp | User: $user | Operation: $operation | Parameters: $parameters | Execution Time: ${executionTime}ms | Status: FAILED | Error: ${e.message}"
            auditLogger.error(logMessage)
            throw e
        }
    }
}
