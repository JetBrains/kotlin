import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
    `jvm-test-suite`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

pill {
    variant = PillExtension.Variant.FULL
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AdvancedKotlinGradlePluginApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
    languageSettings.optIn("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin-model"))
    commonApi(project(":kotlin-tooling-core"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":compiler:incremental-compilation-impl"))
    commonCompileOnly(project(":daemon-common"))
    commonCompileOnly(project(":kotlin-gradle-compiler-types"))
    commonCompileOnly(project(":native:kotlin-native-utils"))
    commonCompileOnly(project(":kotlin-android-extensions"))
    commonCompileOnly(project(":kotlin-build-common"))
    commonCompileOnly(project(":kotlin-compiler-runner"))
    commonCompileOnly(project(":kotlin-annotation-processing"))
    commonCompileOnly(project(":kotlin-annotation-processing-gradle"))
    commonCompileOnly(project(":kotlin-scripting-compiler"))
    commonCompileOnly(project(":kotlin-gradle-statistics"))
    commonCompileOnly(project(":kotlin-gradle-build-metrics"))
    commonCompileOnly("com.android.tools.build:gradle:3.6.4")
    commonCompileOnly("com.android.tools.build:gradle-api:3.6.4")
    commonCompileOnly("com.android.tools.build:builder:3.6.4")
    commonCompileOnly("com.android.tools.build:builder-model:3.6.4")
    commonCompileOnly("org.codehaus.groovy:groovy-all:2.4.12")
    commonCompileOnly(intellijCore())
    commonCompileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    commonCompileOnly("com.gradle:gradle-enterprise-gradle-plugin:3.11.2")
    commonCompileOnly(commonDependency("com.google.code.gson:gson"))
    commonCompileOnly(commonDependency("com.google.guava:guava"))
    commonCompileOnly("de.undercouch:gradle-download-task:4.1.1")
    commonCompileOnly("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }
    commonCompileOnly(project(":kotlin-tooling-metadata"))

    commonImplementation(project(":kotlin-gradle-plugin-idea"))
    commonImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    commonImplementation(project(":kotlin-util-klib"))
    commonImplementation(project(":native:kotlin-klib-commonizer-api"))
    commonImplementation(project(":kotlin-project-model"))

    commonRuntimeOnly(project(":kotlin-compiler-embeddable"))
    commonRuntimeOnly(project(":kotlin-annotation-processing-gradle"))
    commonRuntimeOnly(project(":kotlin-android-extensions"))
    commonRuntimeOnly(project(":kotlin-compiler-runner"))
    commonRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    commonRuntimeOnly(project(":kotlin-scripting-compiler-impl-embeddable"))

    embedded(project(":kotlin-gradle-build-metrics"))
    embedded(project(":kotlin-gradle-statistics"))
    embedded(commonDependency("org.jetbrains.intellij.deps:asm-all")) { isTransitive = false }
    embedded(commonDependency("com.google.code.gson:gson")) { isTransitive = false }
    embedded(commonDependency("com.google.guava:guava")) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.teamcity:serviceMessages")) { isTransitive = false }
    embedded(project(":kotlin-tooling-metadata")) { isTransitive = false }
    embedded("de.undercouch:gradle-download-task:4.1.1")
    embedded("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    testCompileOnly(project(":compiler"))
    testCompileOnly(project(":kotlin-annotation-processing"))
    testCompileOnly(project(":kotlin-annotation-processing-gradle"))

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(projectTests(":kotlin-build-common"))
    testImplementation(project(":kotlin-android-extensions"))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-tooling-metadata"))
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configurations.commonApi.get().exclude("com.android.tools.external.com-intellij", "intellij-core")
}

tasks {
    named<ProcessResources>("processCommonResources") {
        val propertiesToExpand = mapOf(
            "projectVersion" to project.version,
            "kotlinNativeVersion" to project.kotlinNativeVersion
        )
        for ((name, value) in propertiesToExpand) {
            inputs.property(name, value)
        }
        filesMatching("project.properties") {
            expand(propertiesToExpand)
        }
    }

    withType<ValidatePlugins>().configureEach {
        failOnWarning.set(true)
        enableStricterValidation.set(true)
    }

    withType<ShadowJar>().configureEach {
        relocate("com.github.gundy", "$kotlinEmbeddableRootPackage.com.github.gundy")
        relocate("de.undercouch.gradle.tasks.download", "$kotlinEmbeddableRootPackage.de.undercouch.gradle.tasks.download")

        // don't expose external Gradle plugin marker
        // workaround from https://github.com/johnrengelman/shadow/issues/505#issuecomment-644098082
        transform(DontIncludeResourceTransformer::class.java) {
            resource = "META-INF/gradle-plugins/de.undercouch.download.properties"
        }
    }
}

projectTest {
    dependsOn(tasks.named("validatePlugins"))

    workingDir = rootDir
}

gradlePlugin {
    plugins {
        create("kotlinJvmPlugin") {
            id = "org.jetbrains.kotlin.jvm"
            description = "Kotlin JVM plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper"
        }
        create("kotlinJsPlugin") {
            id = "org.jetbrains.kotlin.js"
            description = "Kotlin JS plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinJsPluginWrapper"
        }
        create("kotlinMultiplatformPlugin") {
            id = "org.jetbrains.kotlin.multiplatform"
            description = "Kotlin Multiplatform plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper"
        }
        create("kotlinAndroidPlugin") {
            id = "org.jetbrains.kotlin.android"
            description = "Kotlin Android plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper"
        }
        create("kotlinAndroidExtensionsPlugin") {
            id = "org.jetbrains.kotlin.android.extensions"
            description = "Kotlin Android Extensions plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.AndroidExtensionsSubpluginIndicator"
        }
        create("kotlinParcelizePlugin") {
            id = "org.jetbrains.kotlin.plugin.parcelize"
            description = "Kotlin Parcelize plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.ParcelizeSubplugin"
        }
        create("kotlinKaptPlugin") {
            id = "org.jetbrains.kotlin.kapt"
            description = "Kotlin Kapt plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin"
        }
        create("kotlinScriptingPlugin") {
            id = "org.jetbrains.kotlin.plugin.scripting"
            description = "Gradle plugin for kotlin scripting"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin"
        }
        create("kotlinNativeCocoapodsPlugin") {
            id = "org.jetbrains.kotlin.native.cocoapods"
            description = "Kotlin Native plugin for CocoaPods integration"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin"
        }
        create("kotlinMultiplatformPluginPm20") {
            id = "org.jetbrains.kotlin.multiplatform.pm20"
            description = "Kotlin Multiplatform plugin with PM2.0"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper"
        }
    }
}

// Gradle plugins functional tests
if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {

    testing {
        suites {
            val functionalTest by registering(JvmTestSuite::class) {
                useJUnit()

                dependencies {
                    implementation(project())
                    implementation("com.android.tools.build:gradle:7.2.1")
                    implementation("com.android.tools.build:gradle-api:7.2.1")
                    compileOnly("com.android.tools:common:30.2.1")
                    implementation(gradleKotlinDsl())
                    implementation(project(":kotlin-gradle-plugin-kpm-android"))
                    implementation(project(":kotlin-gradle-plugin-tcs-android"))
                    implementation(project(":kotlin-tooling-metadata"))
                    implementation(project.dependencies.testFixtures(project(":kotlin-gradle-plugin-idea")))
                    implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
                        (this as ExternalModuleDependency).exclude(group = "*")
                    }
                    implementation("org.reflections:reflections:0.10.2")
                }

                configurations.named(sources.implementationConfigurationName) {
                    extendsFrom(configurations.getByName(mainSourceSet.implementationConfigurationName))
                    extendsFrom(configurations.getByName(testSourceSet.implementationConfigurationName))
                }

                configurations.named(sources.runtimeOnlyConfigurationName) {
                    extendsFrom(configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName))
                    extendsFrom(configurations.getByName(testSourceSet.runtimeOnlyConfigurationName))
                }

                targets.create("functionalRegressionTest") {
                    testTask.configure {
                        include("**/org/jetbrains/kotlin/gradle/regressionTests/**")
                    }
                }

                targets.create("functionalDependencyResolutionTest") {
                    testTask.configure {
                        include("**/org/jetbrains/kotlin/gradle/dependencyResolutionTests/**")
                    }
                }

                targets.create("functionalUnitTest") {
                    testTask.configure {
                        include("**/org/jetbrains/kotlin/gradle/unitTests/**")
                    }
                }

                targets.all {
                    testTask.configure {
                        dependsOnKotlinGradlePluginInstall()

                        testLogging {
                            events("passed", "skipped", "failed")
                        }

                        javaLauncher.set(javaToolchains.launcherFor {
                            languageVersion.set(JavaLanguageVersion.of(11))
                        })
                    }
                }

                // open internal visibility to the test code
                kotlin.target.compilations {
                    named(name) {
                        associateWith(this@compilations.getByName("main"))
                        associateWith(this@compilations.getByName("common"))
                    }
                }
            }

            tasks.named("check") {
                dependsOn(functionalTest.name)
            }
        }
    }
}
