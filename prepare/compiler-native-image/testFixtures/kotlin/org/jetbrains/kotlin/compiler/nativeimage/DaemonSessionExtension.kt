/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DaemonSessionExtension(private val sessionClass: Class<out DaemonSession>) : BeforeEachCallback {
    private lateinit var session: DaemonSession
    val compilerRunner = DaemonCompilerRunner { session.compileService }

    override fun beforeEach(context: ExtensionContext) {
        session = context.root.getStore(ExtensionContext.Namespace.create(DaemonSessionExtension::class.java))
            .getOrComputeIfAbsent(sessionClass)
    }
}
