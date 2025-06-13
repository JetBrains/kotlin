import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

description = "Kotlin Mock Runtime for Tests"

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val stdlibProjectDir = file("$rootDir/libraries/stdlib")

val builtinsMetadata: Configuration by configurations.creating

dependencies {
    builtinsMetadata(project(":kotlin-stdlib"))
}

val copyCommonSources by task<Sync> {
    from(stdlibProjectDir.resolve("src"))
        .include(
            "kotlin/Annotation.kt",
            "kotlin/Any.kt",
            "kotlin/Array.kt",
            "kotlin/ArrayIntrinsics.kt",
            "kotlin/Arrays.kt",
            "kotlin/Boolean.kt",
            "kotlin/Char.kt",
            "kotlin/CharSequence.kt",
            "kotlin/Collections.kt",
            "kotlin/Comparable.kt",
            "kotlin/Enum.kt",
            "kotlin/Function.kt",
            "kotlin/Iterator.kt",
            "kotlin/Library.kt",
            "kotlin/Nothing.kt",
            "kotlin/Number.kt",
            "kotlin/Primitives.kt",
            "kotlin/String.kt",
            "kotlin/Throwable.kt",
            "kotlin/Unit.kt",
            "kotlin/util/Standard.kt",
            "kotlin/annotations/Multiplatform.kt",
            "kotlin/annotations/WasExperimental.kt",
            "kotlin/internal/Annotations.kt",
            "kotlin/internal/AnnotationsBuiltin.kt",
            "kotlin/concurrent/atomics/AtomicArrays.common.kt",
            "kotlin/concurrent/atomics/Atomics.common.kt",
            "kotlin/contextParameters/Context.kt",
            "kotlin/contextParameters/ContextOf.kt",
            "kotlin/contracts/ContractBuilder.kt",
            "kotlin/contracts/Effect.kt",
        )
    from(stdlibProjectDir.resolve("common/src"))
        .include(
            "kotlin/ExceptionsH.kt",
        )

    into(layout.buildDirectory.dir("src/common"))
}

val copySources by task<Sync> {
    from(stdlibProjectDir.resolve("jvm/runtime"))
        .include(
            "kotlin/NoWhenBranchMatchedException.kt",
            "kotlin/UninitializedPropertyAccessException.kt",
            "kotlin/TypeAliases.kt",
            "kotlin/text/TypeAliases.kt",
        )
    from(stdlibProjectDir.resolve("jvm/src"))
        .include(
            "kotlin/ArrayIntrinsics.kt",
            "kotlin/Unit.kt",
            "kotlin/collections/TypeAliases.kt",
            "kotlin/enums/EnumEntriesJVM.kt",
            "kotlin/io/Serializable.kt",
        )

    from(stdlibProjectDir.resolve("jvm/builtins"))
        .include("*.kt")

    into(layout.buildDirectory.dir("src/jvm"))
}

kotlin {
    jvm {
        compilations {
            val main by getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        moduleName = "kotlin-stdlib"
                        languageVersion = KotlinVersion.KOTLIN_2_2
                        apiVersion = KotlinVersion.KOTLIN_2_2
                        // providing exhaustive list of args here
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xuse-14-inline-classes-mangling-scheme",
                                "-Xno-new-java-annotation-targets",
                                "-Xstdlib-compilation",
                                "-Xdont-warn-on-error-suppression",
                                "-opt-in=kotlin.contracts.ExperimentalContracts",
                                "-opt-in=kotlin.ExperimentalMultiplatform",
                                "-Xcontext-parameters",
                                "-Xcompile-builtins-as-part-of-stdlib",
                                "-Xreturn-value-checker=full"
                            )
                        )
                    }
                }
            }
        }
    }
    sourceSets {
        commonMain {
            kotlin {
                srcDir("common-src")
                srcDir(copyCommonSources)
            }
            dependencies {
                compileOnly(project(":kotlin-stdlib"))
            }
        }
        val jvmMain by getting {
            kotlin {
                srcDir("jvm-src")
                srcDir(copySources)
            }
        }
    }
}

val jvmJar by tasks.existing(Jar::class) {
    archiveAppendix = null
    dependsOn(builtinsMetadata)
    from {
        includeEmptyDirs = false
        builtinsMetadata.files.map {
            zipTree(it).matching { include("**/*.kotlin_builtins") }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifact(jvmJar.get())
        }
    }

    repositories {
        maven(rootProject.layout.buildDirectory.dir("internal/repo"))
    }
}
