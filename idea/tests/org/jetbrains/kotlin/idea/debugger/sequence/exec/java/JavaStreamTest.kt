// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.exec.java

import org.jetbrains.kotlin.idea.debugger.sequence.exec.KotlinTraceEvaluationTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.lib.LibrarySupportProvider


/**
 * @author Vitaliy.Bibaev
 */
class JavaStreamTest : KotlinTraceEvaluationTestCase() {
  override val appName: String = "java"
  override val librarySupport: LibrarySupportProvider = JavaStandardLibrarySupportProvider()

  fun testSimple() {
    doTest(false)
  }
}