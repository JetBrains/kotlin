import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    java
    kotlin("jvm")
    `java-gradle-plugin`
    id("org.jetbrains.dokka")
    id("jps-compatible")
}

apply(from = "functionalTest.gradle.kts")
val functionalTestImplementation by configurations

configure<GradlePluginDevelopmentExtension> {
    isAutomatedPublishing = false
}

publish()

val jarContents by configurations.creating

sourcesJar()
javadocJar()

repositories {
    google()
    maven("https://plugins.gradle.org/m2/")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    compile(project(":kotlin-gradle-plugin-api"))
    compile(project(":kotlin-gradle-plugin-model"))
    compileOnly(project(":compiler"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":daemon-common"))

    compile(kotlinStdlib())
    compile(project(":kotlin-util-klib"))
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

    compile("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    compile("de.undercouch:gradle-download-task:4.0.2")
    implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    compileOnly("com.android.tools.build:gradle:2.0.0")
    compileOnly("com.android.tools.build:gradle-core:2.0.0")
    compileOnly("com.android.tools.build:builder:2.0.0")
    compileOnly("com.android.tools.build:builder-model:2.0.0")
    compileOnly("org.codehaus.groovy:groovy-all:2.4.12")
    compileOnly(gradleApi())

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
    runtimeOnly(projectRuntimeJar(":kotlin-annotation-processing-gradle"))
    runtimeOnly(projectRuntimeJar(":kotlin-android-extensions"))
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-runner"))
    runtimeOnly(projectRuntimeJar(":kotlin-scripting-compiler-embeddable"))
    runtimeOnly(projectRuntimeJar(":kotlin-scripting-compiler-impl-embeddable"))
    runtimeOnly(project(":kotlin-reflect"))

    jarContents(compileOnly(intellijDep()) {
        includeJars("asm-all", "gson", "serviceMessages", rootProject = rootProject)
    })

    // com.android.tools.build:gradle has ~50 unneeded transitive dependencies
    compileOnly("com.android.tools.build:gradle:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:gradle-core:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:builder-model:3.0.0") { isTransitive = false }
    functionalTestImplementation("com.android.tools.build:gradle:4.0.1") {
        because("Functional tests are using APIs from Android. Latest Version is used to avoid NoClassDefFoundError")
    }

    testCompile(intellijDep()) { includeJars("junit", "serviceMessages", rootProject = rootProject) }

    testCompileOnly(project(":compiler"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(project(":kotlin-android-extensions"))
    testCompile(project(":kotlin-compiler-runner"))
    testCompile(project(":kotlin-test::kotlin-test-junit"))
    testCompile("junit:junit:4.12")
    testCompile(project(":kotlin-gradle-statistics"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(project(":kotlin-annotation-processing"))
    testCompileOnly(project(":kotlin-annotation-processing-gradle"))
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configurations.compile.get().exclude("com.android.tools.external.com-intellij", "intellij-core")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler()).configure {
    dependsOn(jarContents)

    from {
        jarContents.asFileTree.map {
            if (it.endsWith(".jar")) zipTree(it)
            else it
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jdkHome = rootProject.extra["JDK_18"] as String
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check")
    }

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

    named<Jar>("jar") {
        callGroovy("manifestAttributes", manifest, project)
    }

    named<ValidateTaskProperties>("validateTaskProperties") {
        failOnWarning = true
    }

    named("install") {
        dependsOn(named("validateTaskProperties"))
    }

    named<DokkaTask>("dokka") {
        outputFormat = "markdown"
        includes = listOf("$projectDir/Module.md")
    }
}

projectTest {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOn(tasks.named("validateTaskProperties"))

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
}

publishPluginMarkers()
