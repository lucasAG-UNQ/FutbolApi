package com.grupob.futbolapi.aspects

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Aspect
@Component
class MetricsAspect(private val meterRegistry: MeterRegistry) {

    @Pointcut("within(com.grupob.futbolapi.webServices..*)")
    fun webServiceMethods() {}

    @Around("webServiceMethods()")
    fun recordMetrics(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()
        val signature = joinPoint.signature
        val controllerName = signature.declaringType.simpleName
        val endpoint = "${controllerName}.${signature.name}"

        val counter = meterRegistry.counter("api.requests.total", "controller", controllerName, "endpoint", endpoint)
        counter.increment()

        try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - start
            val timer = Timer.builder("api.requests.duration")
                .tag("controller", controllerName)
                .tag("endpoint", endpoint)
                .tag("status", "success")
                .register(meterRegistry)
            timer.record(duration, TimeUnit.MILLISECONDS)
            return result
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            val timer = Timer.builder("api.requests.duration")
                .tag("controller", controllerName)
                .tag("endpoint", endpoint)
                .tag("status", "failure")
                .register(meterRegistry)
            timer.record(duration, TimeUnit.MILLISECONDS)
            throw e
        }
    }
}
