@file:Suppress("HasPlatformType")

import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.check
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.test
import org.gradle.kotlin.dsl.testApi
import org.gradle.kotlin.dsl.testImplementation
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.nativeDistribution.asProperties
import org.jetbrains.kotlin.nativeDistribution.llvmDistributionSource
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution
import kotlin.apply

plugins {
    kotlin("jvm")
    id("native-dependencies")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(intellijCore())
    api(project(":core:compiler.common"))
    api(project(":kotlin-tooling-core"))
    api(project(":native:base"))

    testImplementation(project(":native:external-projects-test-utils"))
    testImplementation(project(":kotlin-native:Interop:Indexer"))

    testImplementation(gradleTestKit())

    testApi(libs.junit.jupiter.api)
    testApi(libs.junit.jupiter.engine)
    testApi(libs.junit.jupiter.params)
    testApi(project(":compiler:tests-common", "tests-jar"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

/* Configure tests */

testsJar()

val k1TestRuntimeClasspath by configurations.creating
val analysisApiRuntimeClasspath by configurations.creating

dependencies {
    k1TestRuntimeClasspath(project(":native:objcexport-header-generator-k1"))
    k1TestRuntimeClasspath(projectTests(":native:objcexport-header-generator-k1"))

    analysisApiRuntimeClasspath(project(":native:objcexport-header-generator-analysis-api"))
    analysisApiRuntimeClasspath(projectTests(":native:objcexport-header-generator-analysis-api"))
}

tasks.test.configure {
    enabled = false
}

objCExportHeaderGeneratorTest("testK1", testDisplayNameTag = "K1") {
    classpath += k1TestRuntimeClasspath
}

objCExportHeaderGeneratorTest("testAnalysisApi", testDisplayNameTag = "AA") {
    classpath += analysisApiRuntimeClasspath
}

tasks.check.configure {
    dependsOn("testK1")
    dependsOn("testAnalysisApi")
    dependsOn(":native:objcexport-header-generator-k1:check")
    dependsOn(":native:objcexport-header-generator-analysis-api:check")
}

tasks {
    // Copy-pasted from Indexer build.gradle.kts.
    withType<Test>().configureEach {
        dependsOn(nativeDependencies.llvmDependency)
        jvmArgumentProviders.add(objects.newInstance<TestArgumentProvider>().apply {
            nativeLibraries.from(testCppRuntime)
        })
        val libclangPath = "${nativeDependencies.llvmPath}/" + if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw) {
            "bin/libclang.dll"
        } else {
            "lib/${System.mapLibraryName("clang")}"
        }
        systemProperty("kotlin.native.llvm.libclang", libclangPath)
        systemProperty("kotlin.native.interop.stubgenerator.temp", layout.buildDirectory.dir("stubGeneratorTestTemp").get().asFile)

        // Set the konan.home property because we run the cinterop tool not from a distribution jar
        // so it will not be able to determine this path by itself.
        systemProperty("konan.home", nativeProtoDistribution.root.asFile) // at most target description is required in the distribution.
        systemProperty("kotlin.native.propertyOverrides", llvmDistributionSource.asProperties.entries.joinToString(separator = ";") {
            "${it.key}=${it.value}"
        })
        environment["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
    }
}

