package com.pgs.device.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect that logs the invocation and successful return of service-layer methods.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut matching all methods of every class in the
     * {@code com.pgs.device.service.service} package.
     */
    @Pointcut("execution(* com.pgs.device.service.service.*.*(..))")
    public void serviceMethods() {
    }

    /**
     * Logs the name and arguments of the intercepted method before it is invoked.
     *
     * @param joinPoint the intercepted method
     */
    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Called service method: {} with arguments: {}",
                joinPoint.getSignature().getName(), joinPoint.getArgs());
    }

    /**
     * Logs the name and returned value of the intercepted method after it
     * completes successfully. Not invoked if the method throws an exception.
     *
     * @param joinPoint the intercepted method
     * @param result    the value returned by the method
     */
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Service method: {}, returned: {}",
                joinPoint.getSignature().getName(), result);
    }
}