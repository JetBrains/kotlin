import org.gradle.plugin.compatibility.compatibility

plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

gradlePlugin {
    plugins {
        create("kotlinAllopenPlugin") {
            id = "org.jetbrains.kotlin.plugin.allopen"
            displayName = "Kotlin All Open compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }

        create("kotlinSpringPlugin") {
            id = "org.jetbrains.kotlin.plugin.spring"
            displayName = "Kotlin Spring compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
