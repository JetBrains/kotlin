// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.collection

import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.KotlinPsiChainBuilderTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.lib.collections.KotlinCollectionSupportProvider

class PositiveCollectionBuildTest : KotlinPsiChainBuilderTestCase.Positive("collection/positive") {
    override val kotlinChainBuilder: StreamChainBuilder = KotlinCollectionSupportProvider().chainBuilder

    fun testIntermediateIsLastCall() = doTest()
    fun testOnlyFilterCall() = doTest()
    fun testTerminationCallUsed() = doTest()
    fun testOnlyTerminationCallUsed() = doTest()
}