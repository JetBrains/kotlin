import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

val configurationBuiltins = configurations.create("builtins")
dependencies {
    configurationBuiltins(project(":core:builtins"))
}

lateinit var jvmCoroutinesExperimentalCompilation: KotlinJvmCompilation

val builtinsDir = "${rootDir}/core/builtins"
val builtinsSrcDir = "${buildDir}/builtin-sources"
val builtinsRuntimeSrcDir = "${buildDir}/builtin-sources-for-runtime"

val jsCommonDir = "${projectDir}/../js"
val jsCommonSrcDir = "${jsCommonDir}/src"
val jsV1Dir = "${projectDir}/../js-v1"
val jsSrcDir = "$jsV1Dir/src"


kotlin {
    metadata {
        compilations {
            val main by getting {
                kotlinOptions.freeCompilerArgs += listOf(
                    "-Xinline-classes",
                    "-Xallow-kotlin-package",
                    "-Xallow-result-return-type"
                )
            }
            val coroutinesExperimental by creating {
                kotlinOptions.freeCompilerArgs += listOf(
                    "-Xcoroutines=enable",
                    "-XXLanguage:-ReleaseCoroutines",
                    "-Xallow-kotlin-package",
                    "-Xallow-result-return-type"
                )

                val sourceSet = sourceSets.create("coroutinesExperimental")

                defaultSourceSet {
                    dependencies {
                        implementation(main.compileDependencyFiles + main.output.classesDirs)
                    }
                }
            }
        }

        mavenPublication {
            artifactId = "kotlin-stdlib-mpp-common" // ?
        }
    }
    jvm {
        withJava()
        compilations {
            val main by getting {
                kotlinOptions.freeCompilerArgs += listOf(
                    "-module-name", "kotlin-stdlib",
                    "-Xmultifile-parts-inherit",
                    "-Xnormalize-constructor-calls=enable",
                    "-Xinline-classes",
                    "-Xallow-kotlin-package",
                    "-Xallow-result-return-type"
                )
            }

            val coroutinesExperimental: KotlinJvmCompilation by creating {
                kotlinOptions.freeCompilerArgs += listOf(
                    "-Xallow-kotlin-package",
                    "-Xallow-result-return-type",
                    "-Xmultifile-parts-inherit",
                    "-Xcoroutines=enable",
                    "-XXLanguage:-ReleaseCoroutines",
                    "-module-name", "kotlin-stdlib-coroutines"
                )
                defaultSourceSet {
                    dependencies {
                        implementation(main.compileDependencyFiles + main.output.classesDirs)
                    }
                }
            }
            jvmCoroutinesExperimentalCompilation = coroutinesExperimental
        }
    }
    js {
        browser {
        }
        nodejs {
        }
        compilations {
            all {
                kotlinOptions {
                    main = "noCall"
                    moduleKind = "commonjs"
                }
            }
            val main by getting {
                kotlinOptions {
                    //            outputFile = "${buildDir}/classes/main/kotlin.js"
                    sourceMap = true
                    sourceMapPrefix = "./"
                    freeCompilerArgs += listOf(
                        "-source-map-base-dirs", listOf(builtinsSrcDir, jsSrcDir, jsCommonSrcDir/*, commonSrcDir, commonSrcDir2, unsignedCommonSrcDir*/).map { File(it).absoluteFile }.joinToString(File.pathSeparator),
                        "-Xallow-kotlin-package",
                        "-Xallow-result-return-type",
                        "-Xinline-classes"
                    )
                }
            }
            val runtime by creating {
                kotlinOptions {
                    metaInfo = false
//                    outputFile = "${buildDir}/classes/builtins/kotlin.js"
                    sourceMap = true
                    sourceMapPrefix = "./"
                    freeCompilerArgs += listOf(
                        "-Xallow-kotlin-package"
                    )
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                //            progressiveMode = true
                useExperimentalAnnotation("kotlin.Experimental")
                useExperimentalAnnotation("kotlin.ExperimentalMultiplatform")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }

        }
        commonMain {
            kotlin.apply {
                srcDir("../common/src")
                srcDir("../src")
                srcDir("../unsigned/src")
            }
        }
        val coroutinesExperimental by getting {
            kotlin.srcDir("../coroutines-experimental/src")
        }
        val jvmMain by getting {
            project.configurations.getByName("jvmMainCompileOnly").extendsFrom(configurationBuiltins)
            dependencies {
                api("org.jetbrains:annotations:13.0")
            }
            val jvmSrcDirs = arrayOf(
                "../jvm/src",
                "../jvm/runtime",
                "$builtinsDir/src"
            )
            kotlin.srcDirs(*jvmSrcDirs)
            project.sourceSets["main"].java.srcDirs(*jvmSrcDirs)
        }
        val jvmCoroutinesExperimental by getting {
            dependsOn(coroutinesExperimental)
            kotlin.srcDir("../coroutines-experimental/jvm/src")
        }

        val jsMain by getting {
            kotlin.apply {
                srcDir(builtinsSrcDir)
                srcDir(jsCommonSrcDir)
                srcDir(jsSrcDir)
            }

            val prepareBuiltinsSources by tasks.registering(Copy::class) {
                doFirst {
                    delete(builtinsSrcDir)
                }
                from("${builtinsDir}/native/kotlin") {
                    include("Iterator.kt")
                    include("Collections.kt")
                    include("CharSequence.kt")
                    include("Annotation.kt")
                }
                from("${builtinsDir}/src/kotlin/") {
                    include("annotation/Annotations.kt")
                    include("Function.kt")
                    include("Iterators.kt")
                    include("Range.kt")
                    include("Progressions.kt")
                    include("ProgressionIterators.kt")
                    include("Ranges.kt")
                    include("internal/InternalAnnotations.kt")
                    include("internal/progressionUtil.kt")
                    include("reflect/**/*.kt")
                    include("Unit.kt")
                }
                into(builtinsSrcDir)
            }
            js().compilations["main"].compileKotlinTaskHolder.configure {
                it.dependsOn(prepareBuiltinsSources)
            }
        }
        val jsRuntime by getting {
            kotlin.apply {
                srcDir(builtinsRuntimeSrcDir)
                srcDir("${jsCommonDir}/runtime")
                srcDir("$jsV1Dir/runtime")
            }
            val prepareComparableSources by tasks.registering(Copy::class) {
                doFirst {
                    delete(builtinsRuntimeSrcDir)
                }
                from("${builtinsDir}/native/kotlin") {
                    include("Comparable.kt")
                }
                into(builtinsRuntimeSrcDir)
            }
            js().compilations["runtime"].compileKotlinTaskHolder.configure {
                it.dependsOn(prepareComparableSources)
            }
        }
    }
}

tasks {
    val jvmJar by existing(Jar::class) {
        dependsOn(configurationBuiltins)
        from { zipTree(configurationBuiltins.singleFile) }
        from(jvmCoroutinesExperimentalCompilation.output.classesDirs)
    }

    val jvmSourcesJar by existing(org.gradle.jvm.tasks.Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

//    val compileKotlinJs by existing(org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile::class) {
//    }
}