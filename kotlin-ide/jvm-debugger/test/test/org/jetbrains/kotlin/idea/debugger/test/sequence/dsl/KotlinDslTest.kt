/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.test.sequence.dsl

import com.intellij.debugger.streams.test.DslTestCase
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinCollectionsPeekCallFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinStatementFactory
import org.jetbrains.kotlin.test.KotlinRoot
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class KotlinDslTest : DslTestCase(DslImpl(KotlinStatementFactory(KotlinCollectionsPeekCallFactory()))) {
    override fun getTestDataPath(): String {
        return File(KotlinRoot.DIR, "jvm-debugger/test/testData/sequence/dsl").absolutePath
    }
}