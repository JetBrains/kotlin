import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

description = "Kotlin Mock Runtime for Tests"

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val builtins by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    builtins(project(":core:builtins"))
}
val stdlibProjectDir = file("$rootDir/libraries/stdlib")

val copyCommonSources by task<Sync> {
    from(stdlibProjectDir.resolve("src"))
        .include(
            "kotlin/Annotation.kt",
//            "kotlin/Annotations.kt",
            "kotlin/Any.kt",
            "kotlin/Array.kt",
            "kotlin/Arrays.kt",
//            "kotlin/ArrayIntrinsics.kt",
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
//            "kotlin/annotation/Annotations.kt",
//            "kotlin/annotations/ExperimentalStdlibApi.kt",
            "kotlin/annotations/Multiplatform.kt",
            "kotlin/annotations/WasExperimental.kt",
            "kotlin/internal/Annotations.kt",
            "kotlin/internal/AnnotationsBuiltin.kt",
            "kotlin/contracts/ContractBuilder.kt",
            "kotlin/contracts/Effect.kt",
//            "kotlin/collections/AbstractCollection.kt",
//            "kotlin/collections/AbstractList.kt",
//            "kotlin/reflect/KClassifier.kt",
//            "kotlin/reflect/KClass.kt",
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
            "kotlin/Unit.kt",
            "kotlin/collections/TypeAliases.kt",
//            "kotlin/enums/EnumEntriesSerializationProxy.kt",
            "kotlin/enums/EnumEntriesJVM.kt",
            "kotlin/io/Serializable.kt",
//            "kotlin/util/Exceptions.kt",
        )

    into(layout.buildDirectory.dir("src/jvm"))
}

kotlin {
    jvm {
        compilations {
            val main by getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        moduleName = "kotlin-stdlib"
                        languageVersion = KotlinVersion.KOTLIN_2_0
                        apiVersion = KotlinVersion.KOTLIN_2_0
                        // providing exhaustive list of args here
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xuse-14-inline-classes-mangling-scheme",
                                "-Xno-new-java-annotation-targets",
                                "-Xlink-via-signatures",
                                "-Xstdlib-compilation",
                                "-Xdont-warn-on-error-suppression",
                                "-opt-in=kotlin.contracts.ExperimentalContracts",
                                "-opt-in=kotlin.ExperimentalMultiplatform",
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
    dependsOn(builtins)
    archiveAppendix = null
    from(provider { zipTree(builtins.singleFile) }) { include("kotlin/**") }
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
