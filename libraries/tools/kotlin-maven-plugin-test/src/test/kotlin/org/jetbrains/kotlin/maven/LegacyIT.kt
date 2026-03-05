/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class LegacyIT : KotlinMavenTestBase() {

    @MavenTest
    fun `test-customJdk-1_8-failure`(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    useKotlinDaemon = false,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertCompilationFailed()
                assertBuildLogContains(
                    "[INFO] Overriding JDK home path with",
                    "Unresolved reference 'StackWalker'"
                )
            }
        }
    }

    @MavenTest
    fun `test-customJdk-17-success`(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_17,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_17))
                )
            ) {
                assertJarExistsAndNotEmpty("target/test-customJdk-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-kotlin-java-compilation`(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-helloworld-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-kts-script-compilation`(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld-kts", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-helloworld-kts-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-accessToInternal`(mavenVersion: TestVersions.Maven) {
        testProject("test-accessToInternal", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(2)
                assertJarExistsAndNotEmpty("target/test-accessToInternal-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-allopen-simple`(mavenVersion: TestVersions.Maven) {
        testProject("test-allopen-simple", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("all-open")
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-allopen-simple-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-allopen-spring`(mavenVersion: TestVersions.Maven) {
        testProject("test-allopen-spring", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("spring")
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-allopen-spring-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-apiVersion`(mavenVersion: TestVersions.Maven) {
        testProject("test-apiVersion", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains("Unresolved reference 'new'")
            }
        }
    }

    @MavenTest
    fun `test-bom`(mavenVersion: TestVersions.Maven) {
        testProject("test-bom", mavenVersion) {
            build(
                "dependency:tree",
                "package",
                expectedToFail = false
            ) {
                assertFileExists("target/test-kotlin-bom-1.0-SNAPSHOT.jar")
                val kotlinVersion = context.kotlinVersion
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-test", kotlinVersion, scope = "test")
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-test-junit5", kotlinVersion, scope = "test")
            }
        }
    }

    @MavenTest
    fun `test-empty-argument`(mavenVersion: TestVersions.Maven) {
        testProject("test-empty-argument", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertBuildLogContains("Empty compiler argument passed in the <configuration> section")
            }
        }
    }

    @MavenTest
    fun `test-enable-extensions`(mavenVersion: TestVersions.Maven) {
        testProject("test-enable-extensions", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertTestsPassed(4)
                assertJarExistsAndNotEmpty("target/test-enable-extensions-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptBuildAccess`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptBuildAccess", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "[INFO] kotlin build script accessing build info of test-executeKotlinScriptBuildAccess project",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptCompileError`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptCompileError", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Unresolved reference: compileErrorHere",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptFile`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptFile", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from Kotlin script file!",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptInline`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptInline", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from inline Kotlin script!",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptScriptException`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptScriptException", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "[ERROR] InvocationTargetException: exception from script",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptWithDependencies`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptWithDependencies", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Dependency jar is: junit-4.13.1.jar",
                )
            }
        }
    }

    @MavenTest
    fun `test-executeKotlinScriptWithTemplate`(mavenVersion: TestVersions.Maven) {
        testProject("test-executeKotlinScriptWithTemplate", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from Kotlin script file!"
                )
            }
        }
    }

    @MavenTest
    fun `test-extraArguments`(mavenVersion: TestVersions.Maven) {
        testProject("test-extraArguments", mavenVersion) {
            build(
                "package", "-X",
                expectedToFail = false
            ) {
                assertCompilerArgsContain(
                    "-Xno-inline", "-Xno-optimize", "-Xno-call-assertions",
                    "-Xno-param-assertions",
                )
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-extraArguments-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-jvmTarget`(mavenVersion: TestVersions.Maven) {
        testProject("test-jvmTarget", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertBuildLogContains("Unknown -jvm-target value: 1.4")
            }
        }
    }

    @MavenTest
    fun `test-kapt-annotationProcessorPaths-without-version`(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-kapt-annotationProcessorPaths-without-version", mavenVersion, buildOptions) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("app-with-kapt/target/app-with-kapt-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app-with-kapt/target/generated-sources/kaptKotlin/compile/MyClass.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
            }
        }
    }

    @MavenTest
    fun `test-kapt-generateKotlinCode`(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-kapt-generateKotlinCode", mavenVersion, buildOptions) {
            build(
                "package", "-X",
                expectedToFail = false
            ) {
                assertBuildLogContains(
                    "[INFO] [kapt] Kapt is enabled.",
                    "[INFO] [kapt] Annotation processors: example.ExampleAnnotationProcessor"
                )
                assertJarExistsAndNotEmpty("app/target/app-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/MyClass.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
            }
        }
    }

    @MavenTest
    fun `test-kotlin-dataframe`(mavenVersion: TestVersions.Maven) {
        testProject("test-kotlin-dataframe", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("kotlin-dataframe")
                assertJarExistsAndNotEmpty("target/test-kotlin-dataframe-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-kotlin-version-in-manifest`(mavenVersion: TestVersions.Maven) {
        testProject("test-kotlin-version-in-manifest", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-kotlin-version-in-manifest-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-languageVersion`(mavenVersion: TestVersions.Maven) {
        testProject("test-languageVersion", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains(
                    "The feature \"break continue in inline lambdas\" is only available since language version 2.2"
                )
            }
        }
    }

    @MavenTest
    fun `test-lombok-simple`(mavenVersion: TestVersions.Maven) {
        testProject("test-lombok-simple", mavenVersion, buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("lombok")
                assertJarExistsAndNotEmpty("target/test-lombok-simple-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-lombok-with-kapt`(mavenVersion: TestVersions.Maven) {
        testProject("test-lombok-with-kapt", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("lombok")
                assertJarExistsAndNotEmpty("annotation-processor/target/lombok-kapt-annotation-processor-1.0-SNAPSHOT.jar")
                assertJarExistsAndNotEmpty("app/target/lombok-kapt-app-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app/target/generated-sources/kapt/compile/cats/CatHouseHelper.java"
                ) { "KAPT-generated Java helper class was not found" }
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/cats/CatHouseExtensions.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
                assertFileExists(
                    "app/target/kaptStubs/compile/cats/CatHouse.java"
                ) { "KAPT stub for CatHouse was not found" }
            }
        }
    }

    @MavenTest
    fun `test-moduleName`(mavenVersion: TestVersions.Maven) {
        testProject("test-moduleName", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-moduleName-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-moduleNameDefault`(mavenVersion: TestVersions.Maven) {
        testProject("test-moduleNameDefault", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-moduleNameDefault-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-multimodule`(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    fun `test-multimodule-in-process`(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-in-process", mavenVersion) {
            build(
                "package", "-X",
                environmentVariables = mapOf("MAVEN_OPTS" to "-XX:MaxMetaspaceSize=300M"),
                expectedToFail = false
            ) {
                // 1 classloader for modules 1, 3, 4, 5 (same compiler config)
                // 1 classloader for module 2 (has all-open compiler plugin)
                assertBuildLogLineCount("[DEBUG] Creating classloader", expectedCount = 2)
            }
        }
    }

    @MavenTest
    fun `test-multimodule-srcdir`(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-srcdir", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    fun `test-multimodule-srcdir-absolute`(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-srcdir-absolute", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    fun `test-noarg-jpa`(mavenVersion: TestVersions.Maven) {
        testProject("test-noarg-jpa", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("jpa")
                assertPluginApplied("all-open")
                assertTestsPassed(2)
                assertJarExistsAndNotEmpty("target/test-noarg-jpa-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-noarg-simple`(mavenVersion: TestVersions.Maven) {
        testProject("test-noarg-simple", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("no-arg")
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-noarg-simple-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @MavenVersions(additional = []) // test-extension uses Plexus component model, incompatible with Maven 4
    fun `test-plugins`(mavenVersion: TestVersions.Maven) {
        testProject("test-plugins", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertBuildLogContains(
                    "[INFO] Applied plugin: 'test-me'",
                    "[INFO] Configuring test plugin with arguments",
                    "[INFO] Plugin applied",
                    "[INFO] Option value: my-special-value",
                )
            }
        }
    }

    @MavenTest
    fun `test-power-assert`(mavenVersion: TestVersions.Maven) {
        testProject("test-power-assert", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("power-assert")
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-power-assert-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-reflection`(mavenVersion: TestVersions.Maven) {
        testProject("test-reflection", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-reflection-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-respect-compile-source-root`(mavenVersion: TestVersions.Maven) {
        testProject("test-respect-compile-source-root", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains("Conflicting overloads:")
            }
        }
    }

    @MavenTest
    fun `test-sam-with-receiver-simple`(mavenVersion: TestVersions.Maven) {
        testProject("test-sam-with-receiver-simple", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("sam-with-receiver")
                assertJarExistsAndNotEmpty("target/test-sam-with-receiver-simple-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-suppressWarnings`(mavenVersion: TestVersions.Maven) {
        testProject("test-suppressWarnings", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertBuildLogDoesNotContain("Redundant '?'")
                assertJarExistsAndNotEmpty("target/test-helloworld-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-custom-source-dirs`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-custom-source-dirs", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-disabled`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-disabled", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsNotEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-disabled-via-property`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-disabled-via-property", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsNotEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-enabled`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-enabled", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertFileExists("target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-source-roots`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-source-roots", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertTestsPassed(2)
                assertFileExists("target/classes/test/Calculator.class")
                assertFileExists("target/test-classes/test/CalculatorTest.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-stdlib`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-stdlib", mavenVersion) {
            build(
                "dependency:tree",
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib", context.kotlinVersion)
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    fun `test-smart-defaults-stdlib-exists`(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-stdlib-exists", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    // Kotlin daemon is disabled for JDK 8 tests: the daemon may be reused from a concurrent
    // compilation with a higher JDK, causing incorrect compilation results.
    @MavenTest
    fun `java8-test-classpath`(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-classpath", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    useKotlinDaemon = false,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-classpath-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `java8-test-dagger-maven-example`(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-dagger-maven-example", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    useKotlinDaemon = false,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertJarExistsAndNotEmpty("target/dagger-maven-example-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    fun `java8-test-kapt-allopen`(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-kapt-allopen", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    useKotlinDaemon = false,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertJarExistsAndNotEmpty("target/dagger-maven-example-1.0-SNAPSHOT.jar")
                assertFileExists("target/generated-sources/kapt/compile/coffee/CoffeeMaker_Factory.java")
            }
        }
    }

    @MavenTest
    fun `java8-test-enable-extensions-kapt-allopen`(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-enable-extensions-kapt-allopen", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    useKotlinDaemon = false,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertJarExistsAndNotEmpty("target/test-enable-extensions-kapt-allopen-1.0-SNAPSHOT.jar")
                assertFileExists("target/generated-sources/kapt/compile/coffee/CoffeeMaker_Factory.java")
            }
        }
    }

    @MavenTest
    fun `java9-test-jlink-modular-artifacts`(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-jlink-modular-artifacts", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("jlinked/target/maven-jlink/release")

                val releaseFile = java.io.File(basedir, "jlinked/target/maven-jlink/release")
                val props = java.util.Properties()
                releaseFile.reader().use { props.load(it) }

                var modules = props.getProperty("MODULES")
                    ?: fail("MODULES property missing from release file")

                if (modules.startsWith("\"") && modules.endsWith("\"")) {
                    modules = modules.substring(1, modules.length - 1)
                }

                val moduleSet = modules.split(" ").toSet()
                for (module in listOf(
                    "java.base",
                    "kotlin.stdlib",
                    "kotlin.stdlib.jdk7",
                    "kotlin.stdlib.jdk8",
                    "org.test.modularApp"
                )) {
                    assertTrue(module in moduleSet) {
                        "Expected to find $module in image modules: $modules"
                    }
                }
            }
        }
    }

    @MavenTest
    fun `java9-test-sourceRootsRegisteredSeveralTimes`(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    fun `java17-test-executeKotlinScriptInlineJdkDep`(mavenVersion: TestVersions.Maven) {
        testProject("java17/test-executeKotlinScriptInlineJdkDep", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)
            ) {
                assertScriptGoalDeprecationWarn()
                assertBuildLogContains(
                    "Hello from inline Kotlin script using java!",
                )
            }
        }
    }

    @MavenTest
    fun `java17-test-fileSnapshotMap-overflow`(mavenVersion: TestVersions.Maven) {
        testProject("java17/test-fileSnapshotMap-overflow", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)
            ) {
                assertJarExistsAndNotEmpty("app/target/test-fileSnapshotMap-overflow-app-1.0-SNAPSHOT.jar")
            }
        }
    }
}