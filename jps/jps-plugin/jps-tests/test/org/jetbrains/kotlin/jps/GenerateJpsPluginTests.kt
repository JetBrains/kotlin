/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.incremental.AbstractJsProtoComparisonTest
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("jps/jps-plugin/jps-tests/test", "jps/jps-plugin/testData") {
            testClass<AbstractIncrementalJvmJpsTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model("incremental/multiModule/jvm", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model(
                    "incremental/multiModule/multiplatform/custom", extension = null, excludeParentDirs = true,
                    targetBackend = TargetBackend.JVM_IR
                )
                model("incremental/pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
                model("incremental/withJava", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model(
                    "incremental/classHierarchyAffected", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR
                )
            }

            //actualizeMppJpsIncTestCaseDirs(testDataRoot, "incremental/multiModule/multiplatform/withGeneratedContent")

            testClass<AbstractIncrementalJsJpsTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractMultiplatformJpsTestWithGeneratedContent> {
                model(
                    "incremental/multiModule/multiplatform/withGeneratedContent", extension = null, excludeParentDirs = true,
                    testClassName = "MultiplatformMultiModule", recursive = true
                )
            }

            testClass<AbstractJvmLookupTrackerTest> {
                model("incremental/lookupTracker/jvm", extension = null, recursive = false)
            }
            testClass<AbstractJsLookupTrackerTest> {
                model("incremental/lookupTracker/js", extension = null, recursive = false)
            }
            testClass<AbstractJsKlibLookupTrackerTest> {
                // todo: investigate why lookups are different from non-klib js
                model("incremental/lookupTracker/jsKlib", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalLazyCachesTest> {
                model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
                model("incremental/changeIncrementalOption", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalCacheVersionChangedTest> {
                model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractDataContainerVersionChangedTest> {
                model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
            }
        }

        testGroup("jps/jps-plugin/jps-tests/test", "jps/jps-plugin/testData") {
            fun TestGroup.TestClass.commonProtoComparisonTests() {
                model("comparison/classSignatureChange", extension = null, excludeParentDirs = true)
                model("comparison/classPrivateOnlyChange", extension = null, excludeParentDirs = true)
                model("comparison/classMembersOnlyChanged", extension = null, excludeParentDirs = true)
                model("comparison/packageMembers", extension = null, excludeParentDirs = true)
                model("comparison/unchanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractJvmProtoComparisonTest> {
                commonProtoComparisonTests()
                model("comparison/jvmOnly", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractJsProtoComparisonTest> {
                commonProtoComparisonTests()
                model("comparison/jsOnly", extension = null, excludeParentDirs = true)
            }
        }
    }
}
