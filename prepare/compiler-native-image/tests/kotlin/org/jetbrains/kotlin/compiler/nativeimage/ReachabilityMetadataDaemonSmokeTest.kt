/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.junit.jupiter.api.extension.RegisterExtension

abstract class AbstractReachabilityMetadataDaemonSmokeTest(
    @JvmField @RegisterExtension val daemon: DaemonSessionExtension = DaemonSessionExtension(TracingAgentDaemonSession::class.java),
) : AbstractCompilerSmokeTest(daemon.compilerRunner)

class ReachabilityMetadataDaemonSmokeTest : AbstractReachabilityMetadataDaemonSmokeTest()
