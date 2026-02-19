/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps

import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit4(args) {
        testGroup("jps/jps-plugin/jps-tests/tests-gen", "jps/jps-plugin/testData") {
            fun incrementalJvmTestData(): TestGroup.TestClass.() -> Unit = {
                val targetBackend = TargetBackend.JVM_IR
                val excludePattern = "(^.*Expect.*)|(^companionConstantChanged)|(.*NoJps.*)"
                modelForDirectoryBasedTest(
                    "incremental", "pureKotlin",
                    extension = null,
                    recursive = false,
                    targetBackend = targetBackend,
                    excludedPattern = excludePattern
                )
                modelForDirectoryBasedTest(
                    "incremental",
                    "classHierarchyAffected",
                    extension = null,
                    recursive = false,
                    targetBackend = targetBackend,
                    excludedPattern = excludePattern
                )
                modelForDirectoryBasedTest("incremental", "inlineFunCallSite", extension = null, excludeParentDirs = true, targetBackend = targetBackend)
                modelForDirectoryBasedTest("incremental", "withJava", extension = null, excludeParentDirs = true, targetBackend = targetBackend)
                modelForDirectoryBasedTest(
                    "incremental",
                    "incrementalJvmCompilerOnly",
                    extension = null,
                    excludeParentDirs = true,
                    targetBackend = targetBackend
                )
                modelForDirectoryBasedTest(
                    "incremental/multiModule",
                    "withJavaUsedInKotlin",
                    extension = null,
                    excludeParentDirs = true,
                    targetBackend = targetBackend
                )
            }

            // IR
            testClass<AbstractIncrementalK1JvmJpsTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                modelForDirectoryBasedTest("incremental/multiModule", "jvm", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                modelForDirectoryBasedTest(
                    "incremental", "pureKotlin",
                    extension = null,
                    recursive = false,
                    targetBackend = TargetBackend.JVM_IR,
                    excludedPattern = ".*SinceK2"
                )
                modelForDirectoryBasedTest("incremental", "withJava", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                modelForDirectoryBasedTest("incremental", "inlineFunCallSite", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                modelForDirectoryBasedTest(
                    "incremental", "classHierarchyAffected", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR
                )
            }

            // K2
            testClass<AbstractIncrementalK2JvmJpsTest>(
                init = incrementalJvmTestData()
            )
            testClass<AbstractIncrementalK2LightTreeJvmJpsTest>(
                init = incrementalJvmTestData()
            )
            testClass<AbstractIncrementalK2FirICLightTreeJvmJpsTest>(
                init = incrementalJvmTestData()
            )

            testClass<AbstractJvmLookupTrackerTest> {
                modelForDirectoryBasedTest("incremental/lookupTracker", "jvm", extension = null, recursive = false)
            }
            testClass<AbstractK1JvmLookupTrackerTest> {
                modelForDirectoryBasedTest("incremental/lookupTracker", "jvm", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalLazyCachesTest> {
                modelForDirectoryBasedTest("incremental", "lazyKotlinCaches", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("incremental", "changeIncrementalOption", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalCacheVersionChangedTest> {
                modelForDirectoryBasedTest("incremental", "cacheVersionChanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractDataContainerVersionChangedTest> {
                modelForDirectoryBasedTest("incremental", "cacheVersionChanged", extension = null, excludeParentDirs = true)
            }
        }

        testGroup("jps/jps-plugin/jps-tests/tests-gen", "jps/jps-plugin/testData") {
            fun TestGroup.TestClass.commonProtoComparisonTests() {
                modelForDirectoryBasedTest("comparison", "classSignatureChange", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("comparison", "classPrivateOnlyChange", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("comparison", "classMembersOnlyChanged", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("comparison", "packageMembers", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("comparison", "unchanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractJvmProtoComparisonTest> {
                commonProtoComparisonTests()
                modelForDirectoryBasedTest("comparison", "jvmOnly", extension = null, excludeParentDirs = true)
            }
        }
    }
}
