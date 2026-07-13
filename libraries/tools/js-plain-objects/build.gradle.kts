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
        create("js-plain-objects") {
            id = "org.jetbrains.kotlin.plugin.js-plain-objects"
            displayName = "Kotlin compiler plugin for typed JS-objects library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.jspo.gradle.JsPlainObjectsKotlinGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
