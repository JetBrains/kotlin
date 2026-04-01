import org.gradle.plugin.compatibility.compatibility

plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonImplementation(project(":kotlin-allopen"))

    testImplementation(gradleApi())
    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        create("kotlinNoargPlugin") {
            id = "org.jetbrains.kotlin.plugin.noarg"
            displayName = "Kotlin No Arg compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.noarg.gradle.NoArgGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }

        create("kotlinJpaPlugin") {
            id = "org.jetbrains.kotlin.plugin.jpa"
            displayName = "Kotlin JPA compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.noarg.gradle.KotlinJpaSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
