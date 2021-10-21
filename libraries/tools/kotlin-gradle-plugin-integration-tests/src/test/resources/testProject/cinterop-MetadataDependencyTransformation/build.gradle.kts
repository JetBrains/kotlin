import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
