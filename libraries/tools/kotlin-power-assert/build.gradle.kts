import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":kotlin-power-assert-compiler-plugin"))
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