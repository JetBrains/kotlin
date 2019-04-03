
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

publish()

// todo: make lazy
val jar: Jar by tasks
val jarContents by configurations.creating

sourcesJar()
javadocJar()

repositories {
    google()
    maven(url = "https://plugins.gradle.org/m2/")
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
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-android-extensions"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":kotlin-compiler-runner"))
    compileOnly(project(":kotlin-annotation-processing"))
    compileOnly(project(":kotlin-annotation-processing-gradle"))
    compileOnly(project(":kotlin-scripting-compiler-impl"))

    compile("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    
    compileOnly("com.android.tools.build:gradle:2.0.0")
    compileOnly("com.android.tools.build:gradle-core:2.0.0")
    compileOnly("com.android.tools.build:builder:2.0.0")
    compileOnly("com.android.tools.build:builder-model:2.0.0")
    compileOnly("org.codehaus.groovy:groovy-all:2.4.12")
    compileOnly(gradleApi())

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
    runtime(projectRuntimeJar(":kotlin-annotation-processing-gradle"))
    runtime(projectRuntimeJar(":kotlin-android-extensions"))
    runtime(projectRuntimeJar(":kotlin-compiler-runner"))
    runtime(projectRuntimeJar(":kotlin-scripting-compiler-embeddable"))
    runtime(projectRuntimeJar(":kotlin-scripting-compiler-impl-embeddable"))
    runtime(project(":kotlin-reflect"))

    jarContents(compileOnly(intellijDep()) {
        includeJars("asm-all", "serviceMessages", "gson", rootProject = rootProject)
    })
    
    jarContents(compileOnly(commonDep("org.jetbrains.kotlin:kotlin-native-shared")) {
        isTransitive = false
    })

    // com.android.tools.build:gradle has ~50 unneeded transitive dependencies
    compileOnly("com.android.tools.build:gradle:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:gradle-core:3.0.0") { isTransitive = false }
    compileOnly("com.android.tools.build:builder-model:3.0.0") { isTransitive = false }

    testCompileOnly (project(":compiler"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(project(":kotlin-android-extensions"))
    testCompile(project(":kotlin-compiler-runner"))
    testCompile(project(":kotlin-test::kotlin-test-junit"))
    testCompile("junit:junit:4.12")
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(project(":kotlin-annotation-processing"))
    testCompileOnly(project(":kotlin-annotation-processing-gradle"))
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configurations.compile.get().exclude("com.android.tools.external.com-intellij", "intellij-core")
}

runtimeJar(rewriteDepsToShadedCompiler(jar)) {
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
        kotlinOptions.languageVersion = "1.2"
        kotlinOptions.apiVersion = "1.2"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    named<ProcessResources>("processResources") {
        val propertiesToExpand = mapOf(
            "projectVersion" to project.version,
            "kotlinNativeVersion" to project.kotlinNativeVersion,
            "kotlinNativeSharedVersion" to project.kotlinNativeSharedVersion
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

    named<DokkaTask>("dokka") {
        outputFormat = "markdown"
        includes = listOf("$projectDir/Module.md")
    }
}

projectTest {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOn(tasks.named("validateTaskProperties"))
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
        display = "Android"
    )
    create(
        name = "kotlinAndroidExtensionsPlugin",
        id = "org.jetbrains.kotlin.android.extensions",
        display = "Kotlin Android Extensions plugin"
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
