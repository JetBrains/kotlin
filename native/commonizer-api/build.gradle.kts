import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.25")
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
    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

/**
 * TODO: This version hack on migrating period K/N into repository Kotlin, in new build infrostructure zero maintance claus isn't dropped,
 * so for old builds we need to keep this version to string representation till total switch on new infrostructure.
 */
val konanVersion = object: org.jetbrains.kotlin.konan.CompilerVersion by NativeCompilerDownloader.DEFAULT_KONAN_VERSION {
    override fun toString(showMeta: Boolean, showBuild: Boolean) = buildString {
        if (major > 1
            || minor > 5
            || maintenance > 20
        )
            return NativeCompilerDownloader.DEFAULT_KONAN_VERSION.toString(showMeta, showBuild)
        append(major)
        append('.')
        append(minor)
        if (maintenance != 0) {
            append('.')
            append(maintenance)
        }
        if (milestone != -1) {
            append("-M")
            append(milestone)
        }
        if (showMeta) {
            append('-')
            append(meta.metaString)
        }
        if (showBuild && build != -1) {
            append('-')
            append(build)
        }
    }

    override fun toString() = toString(meta != org.jetbrains.kotlin.konan.MetaVersion.RELEASE,
                                       meta != org.jetbrains.kotlin.konan.MetaVersion.RELEASE)
}

tasks.register("downloadNativeCompiler") {
    doFirst {
        NativeCompilerDownloader(project, konanVersion).downloadIfNeeded()
    }
}


projectTest(parallel = false) {
    dependsOn(":dist")
    dependsOn("downloadNativeCompiler")
    workingDir = projectDir
    environment("KONAN_HOME", NativeCompilerDownloader(project, konanVersion).compilerDirectory.absolutePath)
}

runtimeJar()
sourcesJar()
javadocJar()
