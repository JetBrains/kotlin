import org.gradle.plugin.compatibility.compatibility

description = "Kotlin lombok compiler plugin"

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
        create("kotlinLombokPlugin") {
            id = "org.jetbrains.kotlin.plugin.lombok"
            displayName = "Kotlin Lombok plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.lombok.gradle.LombokSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
