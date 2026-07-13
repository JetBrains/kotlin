import org.gradle.plugin.compatibility.compatibility

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

gradlePlugin {
    plugins {
        create("powerAssert") {
            id = "org.jetbrains.kotlin.plugin.power-assert"
            displayName = "Kotlin Power-Assert compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradlePlugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
