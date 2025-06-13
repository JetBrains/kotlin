plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))
}

gradlePlugin {
    plugins {
        create("powerAssert") {
            id = "org.jetbrains.kotlin.plugin.power-assert"
            displayName = "Kotlin Power-Assert compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradlePlugin"
        }
    }
}

extra["oldCompilerForGradleCompatibility"] = true
