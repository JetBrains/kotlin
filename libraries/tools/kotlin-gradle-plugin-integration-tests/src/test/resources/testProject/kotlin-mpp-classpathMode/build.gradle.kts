import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.provider.ClassPathModeExceptionCollector

plugins {
    kotlin("multiplatform")
}

tasks.create("listCollectedErrors") {
    doFirst {
        val exceptionCollector = serviceOf<ClassPathModeExceptionCollector>()
        logger.quiet("Collected ${exceptionCollector.exceptions.size} exception(s)")
        exceptionCollector.exceptions.forEach { e ->
            logger.log(LogLevel.ERROR, "Exception: ${e.message}", e)
        }
    }
}

error("ERROR DURING CONFIGURATION PHASE")
