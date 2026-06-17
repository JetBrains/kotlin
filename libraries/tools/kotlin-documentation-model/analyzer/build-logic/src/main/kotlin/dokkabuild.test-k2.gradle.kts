/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.utils.declarable
import dokkabuild.utils.jvmJar
import dokkabuild.utils.resolvable

/**
 * Utility to run unit tests for K2 (analysis API), with Java analysis backed by either PSI or symbols.
 */

plugins {
    id("dokkabuild.base")
    id("dokkabuild.java")
    `jvm-test-suite`
}

val symbolsTestImplementation: Configuration by configurations.creating {
    description = "Dependencies for symbols tests (K2)"
    declarable()
}

val symbolsTestImplementationResolver: Configuration by configurations.creating {
    description = "Resolve dependencies for symbols tests (K2)"
    resolvable()
    extendsFrom(symbolsTestImplementation)
    attributes { jvmJar(objects) }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by suites.getting(JvmTestSuite::class) {

            // JUnit tags for Java analysis (PSI vs symbols) are defined with annotations in test classes.
            val onlyJavaPsiTags = listOf("onlyJavaPsi")
            val onlyJavaSymbolsTags = listOf("onlyJavaSymbols")

            // Create a new target for _only_ running test compatible with symbols-analysis (K2).
            val testSymbolsTarget = targets.register("testSymbols") {
                testTask.configure {
                    val excludedTags = onlyJavaSymbolsTags
                    description = "Runs tests using symbols-analysis (K2) (excluding tags: ${excludedTags})"
                    useJUnitPlatform {
                        excludeTags.addAll(excludedTags)
                    }
                    // Analysis dependencies from `symbolsTestImplementation` should precede all other dependencies
                    // in order  to use the shadowed stdlib from the analysis dependencies
                    classpath = symbolsTestImplementationResolver.incoming.files + classpath
                }
            }

            // Create a new target for running tests with enabled experimental symbols java analysis.
            val testJavaSymbolsTarget = targets.register("testJavaSymbols") {
                testTask.configure {
                    val excludedTags = onlyJavaPsiTags
                    description = "Runs tests using symbols-analysis (K2) for java (excluding tags: $excludedTags)"
                    useJUnitPlatform {
                        excludeTags.addAll(excludedTags)
                    }
                    // Analysis dependencies from `symbolsTestImplementation` should precede all other dependencies
                    // in order to use the shadowed stdlib from the analysis dependencies
                    classpath = symbolsTestImplementationResolver.incoming.files + classpath

                    // Enable experimental symbols java analysis
                    systemProperty("org.jetbrains.dokka.analysis.enableExperimentalSymbolsJavaAnalysis", "true")
                }
            }

            // Run all test targets when running :test
            // don't run the task itself, as it's just an aggregate for the test targets.
            // Ideally, we don't really need this `test` target, but it's not possible to remove it.
            targets.named("test") {
                testTask.configure {
                    onlyIf { false }
                    dependsOn(testSymbolsTarget.map { it.testTask })
                    dependsOn(testJavaSymbolsTarget.map { it.testTask })
                }
            }
        }
    }
}
