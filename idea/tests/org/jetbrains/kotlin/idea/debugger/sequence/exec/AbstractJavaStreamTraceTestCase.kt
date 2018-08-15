/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.exec

import com.intellij.debugger.streams.lib.LibrarySupportProvider
import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.JavaStandardLibrarySupportProvider

@Suppress("unused")
abstract class AbstractJavaStreamTraceTestCase : KotlinTraceTestCase() {
    override val librarySupportProvider: LibrarySupportProvider = JavaStandardLibrarySupportProvider()
}