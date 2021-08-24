/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.kpm.FragmentKind.*
import org.jetbrains.kotlin.gradle.util.KpmDependencyResolutionTestCase
import org.jetbrains.kotlin.gradle.kpm.KpmModulePublicationMode.STANDALONE
import org.jetbrains.kotlin.gradle.kpm.TestDependencyKind
import org.jetbrains.kotlin.gradle.kpm.TestDependencyKind.*
import org.jetbrains.kotlin.gradle.kpm.TestKpmModule
import org.junit.Test

@ExperimentalStdlibApi
internal class KpmDependencyResolutionIT : BaseGradleIT() {
    companion object {
        val testCases = buildList {
            add(diamondAuxiliaryModulesTestCase())

            add(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PROJECT, secondDependencyKind = PROJECT))
            add(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PROJECT, secondDependencyKind = PUBLISHED))
            add(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PUBLISHED, secondDependencyKind = PROJECT))
            add(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PUBLISHED, secondDependencyKind = PUBLISHED))

            add(publicationOfMultipleAuxiliaryModulesTestCase(PROJECT))
            add(publicationOfMultipleAuxiliaryModulesTestCase(PUBLISHED))
        }

        private fun diamondAuxiliaryModulesTestCase() = KpmDependencyResolutionTestCase("diamond-union-auxiliary-modules").apply {
            val bottom = project("bottom")
            val bottomForLeft = bottom.module("forLeft") {
                depends(bottom.main, DIRECT)
                makePublic(STANDALONE)
            }
            val bottomForRight = bottom.module("forRight") {
                depends(bottom.main, DIRECT)
                makePublic(STANDALONE)
            }

            val left = project("left") {
                main.apply {
                    depends(bottomForLeft, PUBLISHED)
                    expectVisibilityOfSimilarStructure(bottomForLeft)
                }
            }
            val right = project("right") {
                main.apply {
                    depends(bottom.moduleNamed("forRight"), PUBLISHED)
                    expectVisibilityOfSimilarStructure(bottomForRight)
                }
            }
            project("top") {
                main.apply {
                    depends(left.main, PUBLISHED)
                    depends(right.main, PUBLISHED)
                    expectVisibilityOfSimilarStructure(left.main)
                    expectVisibilityOfSimilarStructure(right.main)
                    expectVisibilityOfSimilarStructure(bottomForLeft)
                    expectVisibilityOfSimilarStructure(bottomForRight)
                    expectVisibilityOfSimilarStructure(bottom.main)
                }
            }
        }

        private fun oneLevelTransitiveDependenciesTestCase(
            firstDependencyKind: TestDependencyKind,
            secondDependencyKind: TestDependencyKind,
        ): KpmDependencyResolutionTestCase =
            KpmDependencyResolutionTestCase("three projects + 'test', a <-($firstDependencyKind)- b <-(${secondDependencyKind})- c").apply {
                projects.withAll {
                    modules.withAll(::configureSimpleFragments)
                    test.makePublic(STANDALONE)
                }

                val a = project("a")
                val b = project("b") {
                    main.depends(a.main, firstDependencyKind)
                    test.depends(a.test, firstDependencyKind)
                }
                val c = project("c") {
                    main.depends(b.main, secondDependencyKind)
                    test.depends(b.test, secondDependencyKind)
                }

                projects.withAll {
                    moduleNamed("test").expectVisibilityOfSimilarStructure(moduleNamed("main"))

                    listOf(b to a, c to a, c to b).forEach { (depending, dependency) ->
                        listOf("main" to listOf("main"), "test" to listOf("main", "test")).forEach { (dependingModule, dependencyModules) ->
                            dependencyModules.forEach { dependencyModule ->
                                depending.moduleNamed(dependingModule)
                                    .expectVisibilityOfSimilarStructure(dependency.moduleNamed(dependencyModule))
                            }
                        }
                    }
                }
            }

        fun publicationOfMultipleAuxiliaryModulesTestCase(dependencyKind: TestDependencyKind) =
            KpmDependencyResolutionTestCase("custom auxiliary module, a <- a.test <- a.integrationTest <-(${dependencyKind})- b").apply {
                val a = project("a") {
                    test.makePublic(STANDALONE)
                    module("integrationTest") {
                        makePublic(STANDALONE)
                        depends(test, DIRECT)
                        expectVisibilityOfSimilarStructure(main)
                        expectVisibilityOfSimilarStructure(test)
                    }
                }
                project("b") {
                    main.apply {
                        depends(a.moduleNamed("integrationTest"), dependencyKind)
                    }
                    modules.withAll { /* both main and test */
                        expectVisibilityOfSimilarStructure(a.main)
                        expectVisibilityOfSimilarStructure(a.test)
                        expectVisibilityOfSimilarStructure(a.moduleNamed("integrationTest"))
                    }
                }
                projects.withAll {
                    modules.withAll(::configureSimpleFragments)
                }
            }

        private fun configureSimpleFragments(module: TestKpmModule) = with(module) {
            val jvmAndLinux = fragment("jvmAndLinux", COMMON_FRAGMENT)
            val native = fragment("native", COMMON_FRAGMENT)
            fragment("linuxX64", LINUXX64_VARIANT) { refines(native, jvmAndLinux) }
            fragment("jvm", JVM_VARIANT) { refines(jvmAndLinux) }
            val ios = fragment("ios", COMMON_FRAGMENT) { refines(native) }
            fragment("iosArm64", IOSARM64_VARIANT) { refines(ios) }
            fragment("iosX64", IOSX64_VARIANT) { refines(ios) }
            // TODO: add host-specific fragments for hosts other than macOS
        }
    }

    @Test
    fun testDiamondAuxiliaryModules() {
        with(PublishAllTestCaseExecutor()) {
            execute(KpmDependencyResolutionTestCase("diamond-union-auxiliary-modules").apply {
                val bottom = project("bottom")
                val bottomForLeft = bottom.module("forLeft") {
                    depends(bottom.main, DIRECT)
                    makePublic(STANDALONE)
                }
                val bottomForRight = bottom.module("forRight") {
                    depends(bottom.main, DIRECT)
                    makePublic(STANDALONE)
                }

                val left = project("left") {
                    main.apply {
                        depends(bottomForLeft, PUBLISHED)
                        expectVisibilityOfSimilarStructure(bottomForLeft)
                    }
                }
                val right = project("right") {
                    main.apply {
                        depends(bottom.moduleNamed("forRight"), PUBLISHED)
                        expectVisibilityOfSimilarStructure(bottomForRight)
                    }
                }
                project("top") {
                    main.apply {
                        depends(left.main, PUBLISHED)
                        depends(right.main, PUBLISHED)
                        expectVisibilityOfSimilarStructure(left.main)
                        expectVisibilityOfSimilarStructure(right.main)
                        expectVisibilityOfSimilarStructure(bottomForLeft)
                        expectVisibilityOfSimilarStructure(bottomForRight)
                        expectVisibilityOfSimilarStructure(bottom.main)
                    }
                }
            })
        }
    }

    @Test
    fun testProjectToProjectDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PROJECT, secondDependencyKind = PROJECT))
        }
    }

    @Test
    fun testProjectToPublishedDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PROJECT, secondDependencyKind = PUBLISHED))
        }
    }

    @Test
    fun testPublishedToProjectDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PUBLISHED, secondDependencyKind = PROJECT))
        }
    }

    @Test
    fun testPublishedToPublishedDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(oneLevelTransitiveDependenciesTestCase(firstDependencyKind = PUBLISHED, secondDependencyKind = PUBLISHED))
        }
    }

    @Test
    fun testAuxillaryModuleWithProjectDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(publicationOfMultipleAuxiliaryModulesTestCase(PROJECT))
        }
    }

    @Test
    fun testAuxillaryModuleWithPublishedDependency() {
        with(PublishAllTestCaseExecutor()) {
            execute(publicationOfMultipleAuxiliaryModulesTestCase(PUBLISHED))
        }
    }
}