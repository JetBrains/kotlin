import gradle.GradlePluginVariant

plugins {
    id("gradle-plugin-common-configuration")
    `jvm-test-suite`
    id("gradle-plugin-api-reference")
}

repositories {
    google()
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin"))
    commonCompileOnly(libs.android.gradle.plugin.gradle.api) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(project(":plugins:compose-compiler-plugin:group-mapping"))
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
    enableForAllGradlePluginVariants()
    failOnWarning = true

    additionalDokkaConfiguration {
        dokkaSourceSets.configureEach {
            includes.from("api-reference-description.md")
            reportUndocumented.set(true)
            perPackageOption {
                matchingRegex.set("org\\.jetbrains\\.kotlin\\.compose\\.compiler\\.gradle\\.model(\$|\\.).*")
                suppress.set(true)
            }
        }
    }
}

testing {
    suites {
        val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit5)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${coreDepsVersion}")
                implementation("org.jetbrains.kotlin:kotlin-test:${coreDepsVersion}")
            }
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
                implementation(gradleKotlinDsl())
                implementation(project(":compiler:cli-base")) { isTransitive = false }
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
                implementation("org.jetbrains.kotlin:kotlin-test:$coreDepsVersion")

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
            configurations.getByName(it).useDependenciesCompiledForGradle(
                GradlePluginVariant.MAXIMUM_SUPPORTED_GRADLE_VARIANT,
                objects,
            )
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

tasks.withType<Test>().configureEach {
    javaLauncher.value(project.getToolchainLauncherFor(JdkMajorVersion.JDK_21_0)).disallowChanges()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion(libs.versions.commons.lang.get())
            because("CVE-2025-48924")
        }
    }
}
