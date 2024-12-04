import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.provider.ClassPathModeExceptionCollector

plugins {
    kotlin("multiplatform")
}

tasks.create("listCollectedErrors") {
    val exceptionCollector = serviceOf<ClassPathModeExceptionCollector>()
    doFirst {
        logger.quiet("Collected ${exceptionCollector.exceptions.size} exception(s)")
        exceptionCollector.exceptions.forEach { e ->
            logger.error("Exception: ${e.message}", e)
        }
    }
}

error("ERROR DURING CONFIGURATION PHASE")
