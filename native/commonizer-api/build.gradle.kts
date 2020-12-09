import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApi()
}

description = "Kotlin KLIB Library Commonizer"
publish()

dependencies {
    api(kotlinStdlib())

    implementation(project(":native:kotlin-native-utils"))
    testCompile(project(":kotlin-test::kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.register("downloadNativeCompiler") {
    doFirst {
        NativeCompilerDownloader(project).downloadIfNeeded()
    }
}

projectTest(parallel = false) {
    dependsOn(":dist")
    dependsOn("downloadNativeCompiler")
    workingDir = projectDir
    environment("KONAN_HOME", NativeCompilerDownloader(project).compilerDirectory.absolutePath)
}

runtimeJar()
sourcesJar()
javadocJar()
