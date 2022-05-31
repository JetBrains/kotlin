import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(from = "functionalTest.gradle.kts")
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
    languageSettings.optIn("org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.plugin.ExperimentalKotlinGradlePluginApi")
}

dependencies {
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin-model"))
    commonApi(project(":kotlin-tooling-core"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":compiler:incremental-compilation-impl"))
    commonCompileOnly(project(":daemon-common"))
    commonCompileOnly(project(":native:kotlin-native-utils"))
    commonCompileOnly(project(":kotlin-reflect-api"))
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
    commonCompileOnly(project(":kotlin-reflect"))
    commonCompileOnly(intellijCore())
    commonCompileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    commonCompileOnly("com.gradle:gradle-enterprise-gradle-plugin:3.9")

    commonImplementation(project(":kotlin-gradle-plugin-idea"))
    commonImplementation(project(":kotlin-util-klib"))
    commonImplementation(project(":native:kotlin-klib-commonizer-api"))
    commonImplementation(project(":kotlin-tooling-metadata"))
    commonImplementation(project(":kotlin-project-model"))
    commonImplementation(commonDependency("com.google.code.gson:gson"))
    commonImplementation(commonDependency("com.google.guava:guava"))
    commonImplementation("de.undercouch:gradle-download-task:4.1.1")
    commonImplementation("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

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

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        "functionalTestImplementation"("com.android.tools.build:gradle:4.0.1") {
            because("Functional tests are using APIs from Android. Latest Version is used to avoid NoClassDefFoundError")
        }
        "functionalTestImplementation"(gradleKotlinDsl())
        "functionalTestImplementation"(project(":kotlin-gradle-plugin-kpm-android"))
        "functionalTestImplementation"(testFixtures(project(":kotlin-gradle-plugin-idea")))
    }

    testCompileOnly(project(":compiler"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(project(":kotlin-annotation-processing"))
    testCompileOnly(project(":kotlin-annotation-processing-gradle"))

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(projectTests(":kotlin-build-common"))
    testImplementation(project(":kotlin-android-extensions"))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
    testImplementation(project(":kotlin-gradle-statistics"))
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

    withType<DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            includes.from("Module.md")
        }
    }
    register("dokka") {
        dependsOn(named("dokkaJavadoc"))
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
