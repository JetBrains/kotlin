import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.36")
    }
    repositories {
        if (cacheRedirectorEnabled)
            maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        else
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    }
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApi()
}

description = "Kotlin KLIB Library Commonizer API"
publish()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":kotlin-test::kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

//tasks.register("downloadNativeCompiler") {
//    doFirst {
//        if (NativeCompilerDownloader(project).compilerDirectory.exists()) return@doFirst
//        NativeCompilerDownloader(project).downloadIfNeeded()
//    }
//}


projectTest(parallel = false) {
    dependsOn(":dist")
//    dependsOn("downloadNativeCompiler")
//    isEnabled = false
    workingDir = projectDir
//    environment("KONAN_HOME", NativeCompilerDownloader(project).compilerDirectory.absolutePath)
}

runtimeJar()
sourcesJar()
javadocJar()
