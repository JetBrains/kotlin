import org.gradle.plugin.compatibility.compatibility

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
}

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))

    compileOnly(project(":kotlin-gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("atomicfu") {
            id = "org.jetbrains.kotlin.plugin.atomicfu"
            displayName = "Kotlin compiler plugin for kotlinx.atomicfu library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.atomicfu.gradle.AtomicfuKotlinGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
