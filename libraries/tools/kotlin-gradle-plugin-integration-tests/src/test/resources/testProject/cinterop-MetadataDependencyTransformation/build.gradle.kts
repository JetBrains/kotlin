import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.concurrent.atomic.AtomicBoolean

plugins {
    kotlin("multiplatform") apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()?.let { kotlin ->
            val compileAll by tasks.creating
            kotlin.targets.all {
                compileAll.dependsOn(
                    provider { compilations.map { it.compileKotlinTaskName } }
                )
            }
        }
    }
}

// Setup configuration resolution hook
// Report configurations that is going to be resolved before the task graph is ready
val isResolutionAllowed = AtomicBoolean(false)
project.gradle.taskGraph.whenReady { isResolutionAllowed.set(true) }

configurations.all {
    incoming.beforeResolve {
        if (isResolutionAllowed.get()) return@beforeResolve
        println("!!!Configuration resolved before Task Graph is ready: $name")
    }
}