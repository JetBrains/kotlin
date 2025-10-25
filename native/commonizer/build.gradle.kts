import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

plugins {
    kotlin("jvm")
    id("project-tests-convention")
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
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":core:compiler.common.native"))
    compileOnly(project(":native:frontend.native"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)

    // This dependency is necessary to keep the right dependency record inside of POM file:
    publishedCompile(project(":kotlin-compiler"))

    api(kotlinStdlib())

    testImplementation(libs.junit4)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":kotlinx-metadata-klib")) { isTransitive = false }
    testImplementation(project(":kotlin-metadata")) { isTransitive = false }
    testImplementation(project(":native:kotlin-klib-commonizer-api"))
    testImplementation(project(":kotlin-tooling-core"))
    testImplementation(intellijCore())
}

optInToK1Deprecation()

val runCommonizer by tasks.registering(JavaExec::class) {
    classpath(configurations.compileOnly, sourceSets.main.get().runtimeClasspath)
    mainClass.set("org.jetbrains.kotlin.commonizer.cli.CommonizerCLI")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }
}

runtimeJar()
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs

tasks.test.configure {
    // Use the bootstrap K/N stdlib for compiling test code samples.
    val nativeDistributionDownloader = NativeCompilerDownloader(project).also { it.downloadIfNeeded() }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        val compilerDirectory = project.layout.dir(
            providers.provider { nativeDistributionDownloader.compilerDirectory }
        )

        classpath.from(compilerDirectory)
        property.set("kotlin.internal.native.test.nativeHome")
    }
}
