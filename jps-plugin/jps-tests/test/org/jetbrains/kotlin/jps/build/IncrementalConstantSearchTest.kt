/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import com.intellij.util.concurrency.FixedFuture
import java.io.File
import java.util.concurrent.Future

class IncrementalConstantSearchTest : AbstractIncrementalJpsTest() {
    fun testJavaConstantChangedUsedInKotlin() {
        doTest("jps-plugin/testData/incremental/custom/javaConstantChangedUsedInKotlin/")
    }

    fun testJavaConstantUnchangedUsedInKotlin() {
        doTest("jps-plugin/testData/incremental/custom/javaConstantUnchangedUsedInKotlin/")
    }

    fun testKotlinConstantChangedUsedInJava() {
        doTest("jps-plugin/testData/incremental/custom/kotlinConstantChangedUsedInJava/")
    }

    fun testKotlinJvmFieldChangedUsedInJava() {
        doTest("jps-plugin/testData/incremental/custom/kotlinJvmFieldChangedUsedInJava/")
    }

    fun testKotlinConstantUnchangedUsedInJava() {
        doTest("jps-plugin/testData/incremental/custom/kotlinConstantUnchangedUsedInJava/")
    }

    fun testKotlinJvmFieldUnchangedUsedInJava() {
        doTest("jps-plugin/testData/incremental/custom/kotlinJvmFieldUnchangedUsedInJava/")
    }

    override val mockConstantSearch: Callbacks.ConstantAffectionResolver?
        get() = object : Callbacks.ConstantAffectionResolver {
            override fun request(
                    ownerClassName: String?,
                    fieldName: String?,
                    accessFlags: Int,
                    fieldRemoved: Boolean,
                    accessChanged: Boolean
            ): Future<Callbacks.ConstantAffection> {
                // We emulate how constant affection service works in IDEA:
                // it is able to find Kotlin usages of Java constant, but can't find Java usages of Kotlin constant
                val affectedFiles =
                        when {
                            ownerClassName == "JavaClass" && fieldName == "CONST" -> listOf(File(workDir, "src/usage.kt"))
                            ownerClassName == "test.Klass" && fieldName == "CONST" -> listOf(File(workDir, "src/Usage.java"))
                            else -> emptyList()
                        }

                return FixedFuture(Callbacks.ConstantAffection(affectedFiles))
            }
        }
}