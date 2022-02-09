import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    embedded(project(":plugins:lombok:lombok-compiler-plugin")) { isTransitive = false }

    api(project(":kotlin-gradle-plugin-model"))
}

projectTest(parallel = true)

tasks {
    withType<KotlinCompile> {
//        kotlinOptions.jdkHome = rootProject.extra["JDK_18"] as String
        kotlinOptions.languageVersion = "1.4"
        kotlinOptions.apiVersion = "1.4"
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xskip-prerelease-check", "-Xsuppress-version-warnings"
        )
    }
}

gradlePlugin {
    plugins {
        create("kotlinLombokPlugin") {
            id = "org.jetbrains.kotlin.plugin.lombok"
            displayName = "Kotlin Lombok plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.lombok.gradle.LombokSubplugin"
        }
    }
}
