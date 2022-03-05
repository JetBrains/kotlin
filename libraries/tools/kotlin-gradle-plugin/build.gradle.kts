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
    compileOnly(gradleKotlinDsl())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":kotlin-gradle-plugin-model"))
    api(project(":kotlin-tooling-core"))

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
