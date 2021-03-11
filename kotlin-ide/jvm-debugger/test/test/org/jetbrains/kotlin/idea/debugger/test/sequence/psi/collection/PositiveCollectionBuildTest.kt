/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.test.sequence.psi.collection

import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import org.jetbrains.kotlin.idea.debugger.test.sequence.KotlinPsiChainBuilderTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.lib.collections.KotlinCollectionSupportProvider
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class PositiveCollectionBuildTest : KotlinPsiChainBuilderTestCase.Positive("collection/positive") {
    override val kotlinChainBuilder: StreamChainBuilder = KotlinCollectionSupportProvider().chainBuilder

    fun testIntermediateIsLastCall() = doTest()
    fun testOnlyFilterCall() = doTest()
    fun testTerminationCallUsed() = doTest()
    fun testOnlyTerminationCallUsed() = doTest()
}