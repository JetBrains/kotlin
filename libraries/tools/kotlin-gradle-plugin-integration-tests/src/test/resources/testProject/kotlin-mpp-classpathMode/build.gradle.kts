import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.provider.ClassPathModeExceptionCollector

plugins {
    kotlin("multiplatform")
}

tasks.create("listCollectedErrors") {
    val exceptions = provider { serviceOf<ClassPathModeExceptionCollector>().exceptions }
    doFirst {
        val exceptions = exceptions.get()
        logger.quiet("Collected ${exceptions.size} exception(s)")
        exceptions.forEach { e ->
            logger.error("Exception: ${e.message}", e)
        }
    }
}

error("ERROR DURING CONFIGURATION PHASE")
