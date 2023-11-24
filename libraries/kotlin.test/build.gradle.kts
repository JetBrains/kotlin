@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
}

description = "Kotlin Test Library"
base.archivesName = "kotlin-test"

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val kotlinTestCapability = "$group:${base.archivesName.get()}:$version" // add to variants with explicit capabilities when the default one is needed, too
val baseCapability = "$group:kotlin-test-framework:$version"
val implCapability = "$group:kotlin-test-framework-impl:$version"

enum class JvmTestFramework {
    JUnit,
    JUnit5,
    TestNG;

    fun lowercase() = name.lowercase()
}
val jvmTestFrameworks = JvmTestFramework.values().toList()

kotlin {

    jvm {
        compilations {
            all {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.empty() // avoid common options set from the root project
                    }
                }
            }
            val main by getting
            val test by getting
            jvmTestFrameworks.forEach { framework ->
                val frameworkMain = create("$framework") {
                    associateWith(main)
                }
                create("${framework}Test") {
                    associateWith(frameworkMain)
                }
            }
            test.associateWith(getByName("JUnit"))
        }
    }
    js {
        if (!kotlinBuildProperties.isTeamcityBuild) {
            browser {}
        }
        nodejs {}
        compilations["main"].compilerOptions.configure {
            freeCompilerArgs.add("-Xir-module-name=kotlin-test")
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        compilations["main"].compilerOptions.configure {
            freeCompilerArgs.add("-Xir-module-name=kotlin-test")
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        compilations["main"].compilerOptions.configure {
            freeCompilerArgs.add("-Xir-module-name=kotlin-test")
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    optIn.add("kotlin.contracts.ExperimentalContracts")
                    freeCompilerArgs.addAll(
                        "-Xallow-kotlin-package",
                        "-Xexpect-actual-classes",
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
            }
        }
        val annotationsCommonMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("annotations-common/src/main/kotlin")
        }
        val assertionsCommonMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("common/src/main/kotlin")
        }
        val commonTest by getting {
            kotlin.srcDir("annotations-common/src/test/kotlin")
            kotlin.srcDir("common/src/test/kotlin")
        }
        val jvmMain by getting {
            dependsOn(assertionsCommonMain)
            kotlin.srcDir("jvm/src/main/kotlin")
        }
        val jvmTest by getting {
            kotlin.srcDir("jvm/src/test/kotlin")
        }
        val jvmJUnit by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("junit/src/main/kotlin")
            resources.srcDir("junit/src/main/resources")
            dependencies {
                api("junit:junit:4.13.2")
            }
        }
        val jvmJUnitTest by getting {
            kotlin.srcDir("junit/src/test/kotlin")
        }
        val jvmJUnit5 by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("junit5/src/main/kotlin")
            resources.srcDir("junit5/src/main/resources")
            dependencies {
                compileOnly("org.junit.jupiter:junit-jupiter-api:5.0.0")
            }
        }
        val jvmJUnit5Test by getting {
            kotlin.srcDir("junit5/src/test/kotlin")
            dependencies {
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }
        val jvmTestNG by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("testng/src/main/kotlin")
            resources.srcDir("testng/src/main/resources")
            dependencies {
                api("org.testng:testng:6.13.1")
            }
        }
        val jvmTestNGTest by getting {
            kotlin.srcDir("testng/src/test/kotlin")
        }
        val jsMain by getting {
            dependsOn(assertionsCommonMain)
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("js/src/main/kotlin")
        }
        val jsTest by getting {
            kotlin.srcDir("js/src/test/kotlin")
        }
        val wasmCommonMain by creating {
            dependsOn(assertionsCommonMain)
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("wasm/src/main/kotlin")
        }
        val wasmJsMain by getting {
            dependsOn(wasmCommonMain)
            kotlin.srcDir("wasm/js/src/main/kotlin")
        }
        val wasmWasiMain by getting {
            dependsOn(wasmCommonMain)
            kotlin.srcDir("wasm/wasi/src/main/kotlin")
        }
    }
}
