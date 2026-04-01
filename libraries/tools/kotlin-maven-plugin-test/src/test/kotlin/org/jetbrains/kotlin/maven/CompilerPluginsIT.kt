/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

class CompilerPluginsIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("KT-77036: No-arg plugin extension loading and options are logged in debug mode")
    fun testNoArgDebugLogging(mavenVersion: TestVersions.Maven) {
        testProject("kotlin-no-arg", mavenVersion) {
            build("compile", "-X") {
                assertFilesExist(
                    "target/classes/org/jetbrains/example/NoArg.class",
                    "target/classes/org/jetbrains/example/SomeClass.class"
                )
                assertBuildLogContains(
                    "Loaded Maven plugin org.jetbrains.kotlin.test.KotlinNoArgMavenPluginExtension",
                    "Plugin options are: plugin:org.jetbrains.kotlin.noarg:annotation=com.my.Annotation"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("All-open plugin makes annotated classes non-final")
    fun testAllopen(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("All-open Spring preset makes @Component classes non-final")
    fun testAllopenSpring(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("JPA preset generates no-arg constructors for classes with JPA annotations")
    fun testNoArgJpa(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("No-arg plugin generates no-arg constructor alongside primary constructor")
    fun testNoArg(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("Sam-with-receiver plugin enables implicit this for annotated SAM interfaces")
    fun testSamWithReceiver(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("Lombok plugin allows Kotlin to access Lombok-generated getters and setters")
    fun testLombok(mavenVersion: TestVersions.Maven) {
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
    @DisplayName("Power-assert plugin produces detailed assertion failure messages")
    fun testPowerAssert(mavenVersion: TestVersions.Maven) {
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
    // TODO KT-84463: test-extension under src/it/test-plugins registers as a Plexus component,
    //  and kotlin-maven-plugin discovers extensions via Plexus container.lookup() (see KotlinCompileMojoBase).
    //  It seems that both need to be migrated to JSR-330 to run this test with Maven 4 - remove @MavenVersions restriction after.
    @MavenVersions(additional = [])
    @DisplayName("Custom compiler plugin is configured, loaded, and executed with correct options")
    fun testCustomPlugin(mavenVersion: TestVersions.Maven) {
        testProject("test-plugins", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("test-me")
                assertBuildLogContains(
                    "[INFO] Configuring test plugin with arguments",
                    "[INFO] Plugin applied",
                    "[INFO] Option value: my-special-value",
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Kotlin-dataframe plugin compiles code using generated column accessors")
    fun testKotlinDataframe(mavenVersion: TestVersions.Maven) {
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
}
