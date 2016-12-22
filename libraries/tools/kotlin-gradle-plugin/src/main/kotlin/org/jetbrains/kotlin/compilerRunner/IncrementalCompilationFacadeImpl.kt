/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationServicesFacade
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.rmi.server.UnicastRemoteObject

internal class IncrementalCompilationFacadeImpl(
        private val environment: GradleIncrementalCompilerEnvironment,
        port: Int = SOCKET_ANY_FREE_PORT
) : IncrementalCompilationServicesFacade,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) {

    override fun areFileChangesKnown(): Boolean {
        return environment.changedFiles is ChangedFiles.Known
    }

    override fun deletedFiles(): List<File> {
        return (environment.changedFiles as ChangedFiles.Known).removed
    }

    override fun modifiedFiles(): List<File> {
        return (environment.changedFiles as ChangedFiles.Known).modified
    }

    override fun reportCompileIteration(files: Iterable<File>, exitCode: Int) {
        environment.reporter.reportCompileIteration(files, ExitCode.values().first { it.code == exitCode })
    }

    override fun reportIC(message: String) {
        environment.reporter.report { message }
    }

    override fun shouldReportIC(): Boolean {
        return environment.reporter.isDebugEnabled
    }

    override fun workingDir(): File {
        return environment.workingDir
    }

    override fun hasAnnotationsFileUpdater(): Boolean {
        return environment.kaptAnnotationsFileUpdater != null
    }

    override fun revert() {
        environment.kaptAnnotationsFileUpdater!!.revert()
    }

    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        val jvmNames = outdatedClassesJvmNames.map { JvmClassName.byInternalName(it) }
        environment.kaptAnnotationsFileUpdater!!.updateAnnotations(jvmNames)
    }
}