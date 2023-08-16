import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

pill {
    variant = PillExtension.Variant.FULL
}

kotlin {
    compilerOptions {
        optIn.addAll(
            listOf(
                "kotlin.RequiresOptIn",
                "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi",
                "org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi",
                "org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi",
                "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
                "org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi",
                "org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi",
            )
        )
        suppressWarnings = true
    }
}

apiValidation {
    publicMarkers.add("org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi")
    publicMarkers.add("org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl")
    nonPublicMarkers.add("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    additionalSourceSets.add("common")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin-model"))

    // Following two dependencies is a workaround for IDEA import to pick-up them correctly
    commonCompileOnly(project(":kotlin-gradle-plugin-api")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
        }
    }
    commonCompileOnly(project(":kotlin-gradle-plugin-model")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-model-common")
        }
    }

    commonCompileOnly(project(":compiler:cli"))
    commonCompileOnly(project(":daemon-common"))
    commonCompileOnly(project(":kotlin-daemon-client"))
    commonCompileOnly(project(":kotlin-gradle-compiler-types"))
    commonCompileOnly(project(":kotlin-compiler-runner-unshaded"))
    commonCompileOnly(project(":kotlin-gradle-statistics"))
    commonCompileOnly(project(":kotlin-gradle-build-metrics"))
    commonCompileOnly("com.android.tools.build:gradle:4.2.2") {
        exclude("org.ow2.asm")
        exclude("net.sf.proguard")
        exclude("net.sf.jopt-simple")
        exclude("com.squareup")
        exclude("com.google.crypto.tink")
        exclude("com.google.guava")
        exclude("com.google.protobuf")
        exclude("com.google.testing.platform")
        exclude("com.android.tools.lint")
        exclude("androidx.databinding")
        exclude("com.android.tools.analytics-library")
        exclude("com.android.tools.build.jetifier")
        exclude("com.android.tools.build", "transform-api")
        exclude("com.android.tools.build", "builder-test-api")
        exclude("com.android.tools.build", "bundletool")
        exclude("com.android.tools.build", "aaptcompiler")
        exclude("com.android.tools.build", "aapt2-proto")
    }
    commonCompileOnly(intellijPlatformUtil())
    commonCompileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    commonCompileOnly(libs.gradle.enterprise.gradlePlugin)
    commonCompileOnly(commonDependency("com.google.code.gson:gson"))
    commonCompileOnly("de.undercouch:gradle-download-task:4.1.1")
    commonCompileOnly("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }
    commonCompileOnly(project(":kotlin-tooling-metadata"))
    commonCompileOnly(project(":compiler:build-tools:kotlin-build-statistics"))
    commonCompileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all")) { isTransitive = false }

    commonImplementation(project(":kotlin-gradle-plugin-idea"))
    commonImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    commonImplementation(project(":native:kotlin-klib-commonizer-api"))
    commonImplementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    commonImplementation(project(":compiler:build-tools:kotlin-build-statistics"))

    commonRuntimeOnly(project(":kotlin-compiler-runner")) {
        // Excluding dependency with not-relocated 'com.intellij' types
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-build-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    commonRuntimeOnly(project(":kotlin-util-klib"))
    commonRuntimeOnly(project(":prepare:kotlin-gradle-plugin-compiler-dependencies"))

    embedded(project(":kotlin-gradle-build-metrics"))
    embedded(project(":kotlin-gradle-statistics"))
    embedded(commonDependency("org.jetbrains.intellij.deps:asm-all")) { isTransitive = false }
    embedded(commonDependency("com.google.code.gson:gson")) { isTransitive = false }
    embedded(libs.guava) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.teamcity:serviceMessages")) { isTransitive = false }
    embedded(project(":kotlin-tooling-metadata")) { isTransitive = false }
    embedded("de.undercouch:gradle-download-task:4.1.1")
    embedded("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        // Adding workaround KT-57317 for Gradle versions where Kotlin runtime <1.8.0
        "mainEmbedded"(project(":kotlin-build-tools-enum-compat"))
        "gradle70Embedded"(project(":kotlin-build-tools-enum-compat"))
        "gradle71Embedded"(project(":kotlin-build-tools-enum-compat"))
        "gradle74Embedded"(project(":kotlin-build-tools-enum-compat"))
        "gradle75Embedded"(project(":kotlin-build-tools-enum-compat"))
        "gradle76Embedded"(project(":kotlin-build-tools-enum-compat"))
    }

    testCompileOnly(project(":compiler"))
    testCompileOnly(project(":kotlin-annotation-processing"))

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(projectTests(":kotlin-build-common"))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(libs.junit4)
    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-tooling-metadata"))
}

configurations.commonCompileClasspath.get().exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")

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

tasks.named("validatePlugins") {
    // We're manually registering and wiring validation tasks for each plugin variant
    enabled = false
}

projectTest {
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
    }
}

// Gradle plugins functional tests
if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {

    val functionalTestSourceSet = sourceSets.create("functionalTest") {
        compileClasspath += mainSourceSet.output
        runtimeClasspath += mainSourceSet.output

        configurations.getByName(implementationConfigurationName) {
            extendsFrom(configurations.getByName(mainSourceSet.implementationConfigurationName))
            extendsFrom(configurations.getByName(testSourceSet.implementationConfigurationName))
        }

        configurations.getByName(runtimeOnlyConfigurationName) {
            extendsFrom(configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName))
            extendsFrom(configurations.getByName(testSourceSet.runtimeOnlyConfigurationName))
        }
    }

    val functionalTestCompilation = kotlin.target.compilations.getByName("functionalTest")
    functionalTestCompilation.compileJavaTaskProvider.configure {
        sourceCompatibility = JavaLanguageVersion.of(11).toString()
        targetCompatibility = JavaLanguageVersion.of(11).toString()
    }
    functionalTestCompilation.compileTaskProvider.configure {
        with(this as KotlinCompile) {
            kotlinJavaToolchain.toolchain.use(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
        }
    }
    functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("main"))
    functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("common"))

    tasks.register<Test>("functionalTest")

    tasks.register<Test>("functionalUnitTest") {
        include("**/org/jetbrains/kotlin/gradle/unitTests/**")
    }

    tasks.register<Test>("functionalRegressionTest") {
        include("**/org/jetbrains/kotlin/gradle/regressionTests/**")
    }

    tasks.register<Test>("functionalDependencyResolutionTest") {
        include("**/org/jetbrains/kotlin/gradle/dependencyResolutionTests/**")
    }

    tasks.withType<Test>().configureEach {
        if (!name.startsWith("functional")) return@configureEach

        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Runs functional tests"
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
        workingDir = projectDir
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        })
        dependsOnKotlinGradlePluginInstall()
        useAndroidSdk()
        doFirst { acceptAndroidSdkLicenses() }
        maxParallelForks = 8

        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    dependencies {
        val implementation = project.configurations.getByName(functionalTestSourceSet.implementationConfigurationName)
        val compileOnly = project.configurations.getByName(functionalTestSourceSet.compileOnlyConfigurationName)

        implementation("com.android.tools.build:gradle:7.4.2")
        implementation("com.android.tools.build:gradle-api:7.4.2")
        compileOnly("com.android.tools:common:30.2.1")
        implementation(gradleKotlinDsl())
        implementation(project(":kotlin-gradle-plugin-tcs-android"))
        implementation(project(":kotlin-tooling-metadata"))
        implementation(project.dependencies.testFixtures(project(":kotlin-gradle-plugin-idea")))
        implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
            exclude(group = "*")
        }
        implementation("org.reflections:reflections:0.10.2")
    }

    tasks.named("check") {
        dependsOn("functionalTest")
    }
}
