/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.Kapt3BaseIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.AGP.AGP_42
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

@DisplayName("android with kapt3 tests")
@AndroidGradlePluginTests
class Kapt3AndroidIT : Kapt3BaseIT() {
    @DisplayName("kapt works with butterknife")
    @GradleAndroidTest
    fun testButterKnife(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-butterknife".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")

                val butterknifeJavaClassesDir = "app/build/intermediates/javac/debug/classes/org/example/kotlin/butterknife/"
                assertFileInProjectExists(butterknifeJavaClassesDir + "SimpleActivity\$\$ViewBinder.class")

                assertFileInProjectExists("app/build/tmp/kotlin-classes/debug/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
            }

            build("assembleDebug") {
                assertTasksUpToDate(":app:compileDebugKotlin", ":app:compileDebugJavaWithJavac")
            }
        }
    }

    @DisplayName("kapt works with dagger")
    @GradleAndroidTest
    fun testDagger(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/DaggerApplicationComponent.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")

                val daggerJavaClassesDir =
                    "app/build/intermediates/javac/debug/classes/com/example/dagger/kotlin/"

                assertFileInProjectExists(daggerJavaClassesDir + "DaggerApplicationComponent.class")

                assertFileInProjectExists("app/build/tmp/kotlin-classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            }
        }
    }

    @DisplayName("KT-15001")
    @GradleAndroidTest
    fun testKt15001(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kt15001".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
            }
        }
    }

    @DisplayName("kapt works with DbFlow")
    @GradleAndroidTest
    fun testDbFlow(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dbflow".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/GeneratedDatabaseHolder.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/AppDatabaseAppDatabase_Database.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/mobi/porquenao/poc/kotlin/core/Item_Table.java")
            }
        }
    }

    @DisplayName("kapt works with realm")
    @GradleAndroidTest
    fun testRealm(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-realm".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/CatRealmProxy.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/CatRealmProxyInterface.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModule.java")
                assertFileInProjectExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModuleMediator.java")
            }
        }
    }

    @DisplayName("kapt works with databinding")
    @GradleAndroidTest
    fun testDatabinding(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-databinding".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            // TODO: remove the `if` when we drop support for [TestVersions.AGP.AGP_42]
            buildJdk = if (jdkVersion.version >= JavaVersion.VERSION_11) jdkVersion.location else File(System.getProperty("jdk11Home"))
        ) {
            setupDataBinding()

            build(
                "assembleDebug", "assembleAndroidTest",
            ) {
                assertKaptSuccessful()
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/databinding/BR.java")

                // databinding compiler v2 was introduced in AGP 3.1.0, was enabled by default in AGP 3.2.0
                assertOutputContains("-Aandroid.databinding.enableV2=1")
                assertFileInProjectNotExists("library/build/generated/source/kapt/debugAndroidTest/android/databinding/DataBinderMapperImpl.java")
                assertFileInProjectExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBindingImpl.java")

                // KT-23866
                assertOutputDoesNotContain("The following options were not recognized by any processor")
            }
        }
    }

    @DisplayName("KT-25374: kapt doesn't fail with anonymous classes with IC")
    @GradleAndroidTest
    fun testICWithAnonymousClasses(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "icAnonymousTypes".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            setupDataBinding()

            build("assembleDebug") {
                assertKaptSuccessful()
            }

            val modifiedSource = javaSourcesDir().resolve("a.kt")
            modifiedSource.modify {
                assert(it.contains("CrashMe2(1000)"))
                it.replace("CrashMe2(1000)", "CrashMe2(2000)")
            }

            build("assembleDebug")
        }
    }

    @DisplayName("static dsl options are passed to kapt")
    @GradleAndroidTest
    fun testStaticDslOptionsPassedToKapt(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("app").buildGradle.appendText(
                """
    
                apply plugin: 'kotlin-kapt'
    
                android {
                    defaultConfig {
                        javaCompileOptions {
                            annotationProcessorOptions {
                                arguments += ["enable.some.test.option": "true"]
                            }
                        }
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertOutputContains(Regex("AP options.*enable\\.some\\.test\\.option=true"))
            }
        }
    }

    @DisplayName("stubs generation is incremental on changes in android variant java sources")
    @GradleAndroidTest
    fun generateStubsTaskShouldRunIncrementallyOnChangesInAndroidVariantJavaSources(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val javaFile = subProject("app").javaSourcesDir().resolve("com/example/dagger/kotlin/Utils.java")
            javaFile.writeText(
                //language=Java
                """
                package com.example.dagger.kotlin;

                class Utils {
                    public String oneMethod() {
                        return "fake!";
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertTasksExecuted(":app:kaptGenerateStubsDebugKotlin")
            }

            javaFile.writeText(
                //language=Java
                """
                package com.example.dagger.kotlin;

                class Utils {
                    public String oneMethod() {
                        return "fake!";
                    }
                    
                    public void anotherMethod() {
                        int one = 1;
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertTasksExecuted(":app:kaptGenerateStubsDebugKotlin")
                assertOutputDoesNotContain(
                    "The input changes require a full rebuild for incremental task ':app:kaptGenerateStubsDebugKotlin'."
                )
            }
        }
    }

    @DisplayName("KT-45532: kapt tasks shouldn't create outputs at configuration time")
    @GradleAndroidTest
    fun kaptTasksShouldNotCreateOutputsOnConfigurationPhase(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("--dry-run", "assembleDebug") {
                assertFileInProjectNotExists("app/build/tmp")
                assertFileInProjectNotExists("app/build/generated")
            }
        }
    }

    @DisplayName("KT-30735: kapt works with androidx.navigation.safeargs")
    @GradleAndroidTest
    fun testAndroidxNavigationSafeArgs(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "androidx-navigation-safe-args".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val safeArgsVersion = if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_7_0)) "2.5.3" else "2.1.0"
            build("assembleDebug", "-Psafe_args_version=$safeArgsVersion") {
                assertFileInProjectExists("build/generated/source/navigation-args/debug/test/androidx/navigation/StartFragmentDirections.java")
                assertFileInProjectExists("build/tmp/kotlin-classes/debug/test/androidx/navigation/StartFragmentKt.class")
            }
        }
    }

    @DisplayName("KT-31127: kapt doesn't break JavaCompile when using Filer API")
    @GradleAndroidTest
    fun testKotlinProcessorUsingFiler(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidLibraryKotlinProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            buildGradle.appendText(
                """
                apply plugin: 'kotlin-kapt'
                android {
                    libraryVariants.all {
                        it.generateBuildConfig.enabled = false
                    }
                }
    
                dependencies {
                    kapt "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                    implementation "org.jetbrains.kotlin:annotation-processor-example:${"$"}kotlin_version"
                }
                """.trimIndent()
            )

            // The test must not contain any java sources in order to detect the issue.
            val javaSources = projectPath.allJavaSources
            assert(javaSources.isEmpty()) {
                "Test project shouldn't contain any java sources, but it contains: $javaSources"
            }
            kotlinSourcesDir().resolve("Dummy.kt").modify {
                it.replace("class Dummy", "@example.KotlinFilerGenerated class Dummy")
            }

            build("assembleDebug") {
                assertFileInProjectExists("build/generated/source/kapt/debug/demo/DummyGenerated.kt")
                assertTasksExecuted(":compileDebugKotlin")
                assertTasksNoSource(":compileDebugJavaWithJavac")
            }
        }
    }

    @DisplayName("inter-project IC works with kapt")
    @GradleAndroidTest
    fun testInterProjectIC(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-inter-project-ic".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
            }

            fun modifyAndCheck(utilKt: Path, useUtilFileName: String) {
                utilKt.modify {
                    it.checkedReplace("Int", "Number")
                }

                build("assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                    val affectedFile = subProject("app").kotlinSourcesDir().resolve("org.example.inter.project.ic")
                        .resolve(useUtilFileName).relativeTo(projectPath)
                    assertCompiledKotlinSources(
                        listOf(affectedFile),
                        getOutputForTask("app:kaptGenerateStubsDebugKotlin"),
                        errorMessageSuffix = " in task ':app:kaptGenerateStubsDebugKotlin"
                    )
                    assertCompiledKotlinSources(
                        listOf(affectedFile),
                        getOutputForTask("app:compileDebugKotlin"),
                        errorMessageSuffix = " in task ':app:compileDebugKotlin"
                    )
                }
            }

            val libAndroidProject = subProject("lib-android")
            modifyAndCheck(libAndroidProject.kotlinSourcesDir().resolve("libAndroidUtil.kt"), "useLibAndroidUtil.kt")
            val libJvmProject = subProject("lib-jvm")
            modifyAndCheck(libJvmProject.kotlinSourcesDir().resolve("libJvmUtil.kt"), "useLibJvmUtil.kt")
        }
    }

    @DisplayName("kapt works with androidx")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = AGP_42, maxVersion = AGP_42)
    fun testDatabindingWithAndroidX(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-databinding-androidX".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("kaptDebugKotlin") {
                assertKaptSuccessful()
            }
        }
    }

    @DisplayName("kapt uses AGP annotation processor option providers as nested inputs")
    @GradleAndroidTest
    fun testKaptUsingApOptionProvidersAsNestedInputOutput(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, kaptOptions = null),
            buildJdk = jdkVersion.location
        ) {
            subProject("Android").buildGradle.appendText(
                //language=Gradle
                """

                apply plugin: 'kotlin-kapt'

                class MyNested implements org.gradle.process.CommandLineArgumentProvider {
                    private final File argsFile
                     
                    MyNested(File argsFile) {
                        this.argsFile = argsFile
                    } 
    
                    @InputFile
                    File inputFile = null
    
                    @Override
                    Iterable<String> asArguments() {
                        // Read the arguments from a file, because changing them in a build script is treated as an
                        // implementation change by Gradle:
                        return [argsFile.text]
                    }
                }
    
                def nested = new MyNested(rootProject.file("args.txt"))
                nested.inputFile = file("${'$'}projectDir/in.txt")
    
                android.applicationVariants.all {
                    it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
                }
                """.trimIndent()
            )

            val inFile = subProject("Android").projectPath.resolve("in.txt")
            inFile.writeText("1234")
            val argsFile = projectPath.resolve("args.txt")
            argsFile.writeText("1234")

            val kaptTasks = listOf(":Android:kaptFlavor1DebugKotlin")
            val javacTasks = listOf(":Android:compileFlavor1DebugJavaWithJavac")

            val buildTasks = (kaptTasks + javacTasks).toTypedArray()

            build(*buildTasks) {
                assertTasksExecuted(kaptTasks + javacTasks)
            }

            inFile.appendText("5678")

            build(*buildTasks) {
                assertTasksExecuted(kaptTasks)
                assertTasksUpToDate(javacTasks)
            }

            // Changing only the annotation provider arguments should not trigger the tasks to run, as the arguments may be outputs,
            // internals or neither:
            argsFile.appendText("5678")

            build(*buildTasks) {
                assertTasksUpToDate(javacTasks + kaptTasks)
            }
        }
    }

    @DisplayName("agp annotation processor nested arguments are not evaluated at configuration time")
    @GradleAndroidTest
    fun testAgpNestedArgsNotEvaluatedDuringConfiguration(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("Android").buildGradle.appendText(
                //language=Gradle
                """

                apply plugin: 'kotlin-kapt'

                class MyNested implements org.gradle.process.CommandLineArgumentProvider {
                    @Override
                    Iterable<String> asArguments() {
                        throw new RuntimeException("This should not be invoked during configuration.")
                    }
                }
    
                def nested = new MyNested()
    
                android.applicationVariants.all {
                    it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
                }
                """.trimIndent()
            )

            build(":Android:kaptFlavor1DebugKotlin", "--dry-run")

            build(
                ":Android:kaptFlavor1DebugKotlin", "--dry-run",
                buildOptions = buildOptions.copy(kaptOptions = BuildOptions.KaptOptions(verbose = false))
            )
        }
    }

    @DisplayName("incremental compilation works with dagger")
    @GradleAndroidTest
    fun testAndroidDaggerIC(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidDaggerProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")

            val androidModuleKt = subProject("app").javaSourcesDir().resolve("com/example/dagger/kotlin/AndroidModule.kt")
            androidModuleKt.modify {
                it.replace(
                    "fun provideApplicationContext(): Context {",
                    "fun provideApplicationContext(): Context? {"
                )
            }

            build(":app:assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(
                    ":app:kaptGenerateStubsDebugKotlin",
                    ":app:kaptDebugKotlin",
                    ":app:compileDebugKotlin",
                    ":app:compileDebugJavaWithJavac"
                )

                // Output is combined with previous build, but we are only interested in the compilation
                // from second build to avoid false positive test failure
                val filteredOutput = output
                    .lineSequence()
                    .filter { it.contains("[KOTLIN] compile iteration:") }
                    .drop(1)
                    .joinToString(separator = "/n")
                assertCompiledKotlinSources(listOf(androidModuleKt).relativizeTo(projectPath), output = filteredOutput)
            }
        }
    }

    @DisplayName("incremental compilation with android and kapt")
    @GradleAndroidTest
    fun testAndroidWithKaptIncremental(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalMultiModule",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val appProject = subProject("app")
            appProject.buildGradle.modify {
                //language=Gradle
                """
                apply plugin: 'org.jetbrains.kotlin.kapt'
                $it
                """.trimIndent()
            }

            build(":app:testDebugUnitTest")

            appProject.kotlinSourcesDir().resolve("com/example/KotlinActivity.kt").appendText(
                //language=kt
                """
                {
                  private val x = 1
                }
                """.trimIndent()
            )

            build(":app:testDebugUnitTest") {
                assertOutputDoesNotContain(NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED)
            }
        }
    }

    private fun TestProject.setupDataBinding() {
        buildGradle.appendText(
            //language=Gradle
            """
                
            allprojects {
                plugins.withId("kotlin-kapt") {
                    println("${'$'}project android.databinding.enableV2=${'$'}{project.findProperty('android.databinding.enableV2')}")

                    // With new AGP, there's no need in the Databinding kapt dependency:
                    configurations.kapt.exclude group: "com.android.databinding", module: "compiler"
                }
            }
            """.trimIndent()
        )
    }
}