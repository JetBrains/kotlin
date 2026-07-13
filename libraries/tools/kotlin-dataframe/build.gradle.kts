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
        create("kotlinDataFrame") {
            id = "org.jetbrains.kotlin.plugin.dataframe"
            displayName = "Kotlin compiler plugin for Kotlin DataFrame library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.dataframe.gradle.DataFrameSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
