import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    java
    `java-gradle-plugin`
    id("gradle-plugin-common-configuration")
    id("org.jetbrains.dokka")
    id("jps-compatible")
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(from = "functionalTest.gradle.kts")
}

configure<GradlePluginDevelopmentExtension> {
    isAutomatedPublishing = false
}

repositories {
    google()
    maven("https://plugins.gradle.org/m2/")
}

pill {
    variant = PillExtension.Variant.FULL
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalVariantApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AdvancedKotlinGradlePluginApi")
    languageSettings.optIn("org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi")
}

dependencies {
    compileOnly(gradleKotlinDsl())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":kotlin-gradle-plugin-model"))
    implementation(project(":kotlin-gradle-plugin-idea"))
    compileOnly(project(":compiler"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":daemon-common"))

    implementation(project(":kotlin-util-klib"))
    implementation(project(":native:kotlin-klib-commonizer-api"))
    implementation(project(":kotlin-tooling-metadata"))
    implementation(project(":kotlin-project-model"))
    compileOnly(project(":native:kotlin-native-utils"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-android-extensions"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":kotlin-compiler-runner"))
    compileOnly(project(":kotlin-annotation-processing"))
    compileOnly(project(":kotlin-annotation-processing-gradle"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(project(":kotlin-gradle-statistics"))
    embedded(project(":kotlin-gradle-statistics"))
    compileOnly(project(":kotlin-gradle-build-metrics"))
    embedded(project(":kotlin-gradle-build-metrics"))

    implementation(commonDependency("com.google.code.gson:gson"))
    implementation(commonDependency("com.google.guava:guava"))

    implementation("de.undercouch:gradle-download-task:4.1.1")
    implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    compileOnly("com.android.tools.build:gradle:3.4.0")
    compileOnly("com.android.tools.build:gradle-api:3.4.0")
    compileOnly("com.android.tools.build:builder:3.4.0")
    compileOnly("com.android.tools.build:builder-model:3.4.0")
    compileOnly("org.codehaus.groovy:groovy-all:2.4.12")
    compileOnly(project(":kotlin-reflect"))
    compileOnly(intellijCore())

    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-annotation-processing-gradle"))
    runtimeOnly(project(":kotlin-android-extensions"))
    runtimeOnly(project(":kotlin-compiler-runner"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    runtimeOnly(project(":kotlin-scripting-compiler-impl-embeddable"))

    compileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))

    embedded(commonDependency("org.jetbrains.intellij.deps:asm-all")) { isTransitive = false }
    embedded(commonDependency("com.google.code.gson:gson")) { isTransitive = false }
    embedded(commonDependency("com.google.guava:guava")) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.teamcity:serviceMessages")) { isTransitive = false }

    // com.android.tools.build:gradle has ~50 unneeded transitive dependencies
    compileOnly("com.android.tools.build:gradle:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:gradle-core:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:builder-model:3.0.0") { isTransitive = false }

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        "functionalTestImplementation"("com.android.tools.build:gradle:4.0.1") {
            because("Functional tests are using APIs from Android. Latest Version is used to avoid NoClassDefFoundError")
        }
        "functionalTestImplementation"(gradleKotlinDsl())
    }

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))

    testCompileOnly(project(":compiler"))
    testImplementation(projectTests(":kotlin-build-common"))
    testImplementation(project(":kotlin-android-extensions"))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
    testImplementation(project(":kotlin-gradle-statistics"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(project(":kotlin-annotation-processing"))
    testCompileOnly(project(":kotlin-annotation-processing-gradle"))

    compileOnly("com.gradle:gradle-enterprise-gradle-plugin:3.6.3")
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configurations.api.get().exclude("com.android.tools.external.com-intellij", "intellij-core")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

tasks {
    named<ProcessResources>("processResources") {
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

    named("install") {
        dependsOn(named("validatePlugins"))
    }

    named<DokkaTask>("dokka") {
        outputFormat = "markdown"
        includes = listOf("$projectDir/Module.md")
    }
}

projectTest {
    dependsOn(tasks.named("validatePlugins"))

    workingDir = rootDir
}

pluginBundle {
    fun create(name: String, id: String, display: String) {
        (plugins).create(name) {
            this.id = id
            this.displayName = display
            this.description = display
        }
    }

    create(
        name = "kotlinJvmPlugin",
        id = "org.jetbrains.kotlin.jvm",
        display = "Kotlin JVM plugin"
    )
    create(
        name = "kotlinJsPlugin",
        id = "org.jetbrains.kotlin.js",
        display = "Kotlin JS plugin"
    )
    create(
        name = "kotlinMultiplatformPlugin",
        id = "org.jetbrains.kotlin.multiplatform",
        display = "Kotlin Multiplatform plugin"
    )
    create(
        name = "kotlinAndroidPlugin",
        id = "org.jetbrains.kotlin.android",
        display = "Kotlin Android plugin"
    )
    create(
        name = "kotlinAndroidExtensionsPlugin",
        id = "org.jetbrains.kotlin.android.extensions",
        display = "Kotlin Android Extensions plugin"
    )
    create(
        name = "kotlinParcelizePlugin",
        id = "org.jetbrains.kotlin.plugin.parcelize",
        display = "Kotlin Parcelize plugin"
    )
    create(
        name = "kotlinKaptPlugin",
        id = "org.jetbrains.kotlin.kapt",
        display = "Kotlin Kapt plugin"
    )
    create(
        name = "kotlinScriptingPlugin",
        id = "org.jetbrains.kotlin.plugin.scripting",
        display = "Gradle plugin for kotlin scripting"
    )
    create(
        name = "kotlinNativeCocoapodsPlugin",
        id = "org.jetbrains.kotlin.native.cocoapods",
        display = "Kotlin Native plugin for CocoaPods integration"
    )
    create(
        name = "kotlinMultiplatformPluginPm20",
        id = "org.jetbrains.kotlin.multiplatform.pm20",
        display = "Kotlin Multiplatform plugin with PM2.0"
    )
}

publishPluginMarkers()
