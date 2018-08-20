// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.dsl

import com.intellij.debugger.streams.test.DslTestCase
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinCollectionsPeekCallFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinStatementFactory

class KotlinDslTest : DslTestCase(DslImpl(KotlinStatementFactory(KotlinCollectionsPeekCallFactory()))) {
    override fun getTestDataPath(): String {
        return "idea/testData/debugger/sequence/dsl"
    }
}