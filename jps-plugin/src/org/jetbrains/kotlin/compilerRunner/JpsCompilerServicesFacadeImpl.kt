/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.daemon.client.CompilerCallbackServicesFacadeServer
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.Serializable

internal class JpsCompilerServicesFacadeImpl(
        private val env: JpsCompilerEnvironment,
        port: Int = SOCKET_ANY_FREE_PORT
) : CompilerCallbackServicesFacadeServer(env.services.get(IncrementalCompilationComponents::class.java),
                                         env.services.get(LookupTracker::class.java),
                                         env.services.get(CompilationCanceledStatus::class.java),
                                         port),
        JpsCompilerServicesFacade {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        env.messageCollector.reportFromDaemon(
                { outFile, srcFiles -> env.outputItemsCollector.add(srcFiles, outFile) },
                category, severity, message, attachment)
    }
}