import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

description = "Kotlin KLIB Library Commonizer"

publish()

configurations {
    testRuntimeOnly {
        extendsFrom(compileOnly.get())
    }
}

dependencies {
    embedded(project(":kotlinx-metadata-klib")) { isTransitive = false }
    embedded(project(":kotlin-metadata")) { isTransitive = false }
    embedded(project(":native:kotlin-klib-commonizer-api")) { isTransitive = false }
    embedded(project(":kotlin-tooling-core")) { isTransitive = false }

    // N.B. The order of "kotlinx-metadata*" dependencies makes sense for runtime classpath
    // of the "runCommonizer" task. Please, don't mix them up.
    compileOnly(project(":kotlinx-metadata-klib")) { isTransitive = false }
    compileOnly(project(":kotlin-metadata")) { isTransitive = false }
    compileOnly(project(":native:kotlin-klib-commonizer-api")) { isTransitive = false }
    compileOnly(project(":kotlin-tooling-core")) { isTransitive = false }
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":core:compiler.common.native"))
    compileOnly(project(":native:frontend.native"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)

    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:deserialization"))
    compileOnly(project(":core:deserialization.common"))

    // This dependency is necessary to keep the right dependency record inside of POM file:
    publishedCompile(project(":kotlin-compiler"))

    api(kotlinStdlib())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":kotlinx-metadata-klib")) { isTransitive = false }
    testImplementation(project(":kotlin-metadata")) { isTransitive = false }
    testImplementation(project(":native:kotlin-klib-commonizer-api"))
    testImplementation(project(":kotlin-tooling-core"))
    testImplementation(project(":native:native.config"))
    testImplementation(intellijCore())

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val runCommonizer = tasks.register("runCommonizer", JavaExec::class) {
    classpath(configurations.compileOnly, sourceSets.main.get().runtimeClasspath)
    mainClass = "org.jetbrains.kotlin.commonizer.cli.CommonizerCLI"
}

sourceSets {
    main { projectDefault() }
    test { projectDefault() }
}

projectTests {
    testTask {
        // Use the bootstrap K/N stdlib for compiling test code samples.
        val nativeDistributionDownloader = NativeCompilerDownloader(project).also { it.downloadIfNeeded() }
        val compilerDirectory = project.layout.dir(providers.provider { nativeDistributionDownloader.compilerDirectory })
        addClasspathProperty("kotlin.internal.native.test.nativeHome") { from(compilerDirectory) }
    }
    testData(project.isolated, "testData")
}

runtimeJar()
emptySourcesJar()
emptyJavadocJar()
