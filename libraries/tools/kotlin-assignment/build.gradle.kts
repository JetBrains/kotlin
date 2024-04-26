plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":kotlin-assignment-compiler-plugin"))

    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        create("assignment") {
            id = "org.jetbrains.kotlin.plugin.assignment"
            displayName = "Kotlin Assignment compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentSubplugin"
        }
    }
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter(libs.versions.junit5)
            }

            register<JvmTestSuite>("functionalTest") {
                dependencies {
                    implementation(project())
                    implementation(gradleKotlinDsl())
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
        }
    }

    tasks.named("check") {
        dependsOn(testing.suites.named("functionalTest"))
    }
}
