import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    compileOnly(kotlinStdlib())
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
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
