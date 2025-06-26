import gradle.GradlePluginVariant

plugins {
    id("gradle-plugin-common-configuration")
    `jvm-test-suite`
    id("gradle-plugin-api-reference")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))
    commonApi(project(":kotlin-gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("kotlinComposeCompilerPlugin") {
            id = "org.jetbrains.kotlin.plugin.compose"
            displayName = "Compose Compiler Gradle plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin"
        }
    }
}

pluginApiReference {
    enableForGradlePluginVariants(GradlePluginVariant.values().toSet())
    enableKotlinlangDocumentation()

    failOnWarning = true

    additionalDokkaConfiguration {
        reportUndocumented.set(true)
        perPackageOption {
            matchingRegex.set("org\\.jetbrains\\.kotlin\\.compose\\.compiler\\.gradle\\.model(\$|\\.).*")
            suppress.set(true)
        }
    }
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter(libs.versions.junit5)
                dependencies {
                    implementation(project(":kotlin-test"))
                }
            }

            register<JvmTestSuite>("functionalTest") {
                dependencies {
                    implementation(project())
                    implementation(gradleKotlinDsl())
                    implementation(project(":compiler:cli-common"))
                    implementation(platform(libs.junit.bom))
                    implementation(libs.junit.jupiter.api)
                    implementation(project(":kotlin-test"))

                    runtimeOnly(libs.junit.jupiter.engine)
                }

                targets {
                    all {
                        testTask.configure {
                            shouldRunAfter(test)
                        }
                    }
                }
            }

            val functionalTests = sourceSets.getByName("functionalTest")
            listOf(
                functionalTests.compileClasspathConfigurationName,
                functionalTests.runtimeClasspathConfigurationName,
            ).forEach {
                configurations.getByName(it).attributes.attribute(
                    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                    objects.named(GradlePluginVariant.values().maxBy { GradleVersion.version(it.minimalSupportedGradleVersion) }.minimalSupportedGradleVersion)
                )
            }
        }
    }

    tasks.named("check") {
        dependsOn(testing.suites.named("functionalTest"))
    }
}
