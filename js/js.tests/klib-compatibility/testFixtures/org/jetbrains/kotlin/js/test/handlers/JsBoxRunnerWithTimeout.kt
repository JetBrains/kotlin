/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers
 
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.util.concurrent.*
import java.security.AccessController
import java.security.PrivilegedAction

/*
 * A wrapper over JsBoxRunner with a timeout
 */
class JsBoxRunnerWithTimeout(testServices: TestServices, private val timeoutMillis: Long): AbstractJsArtifactsCollector(testServices) {
    val jsBoxRunner = JsBoxRunner(testServices)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        jsBoxRunner.processModule(module, info)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val context = AccessController.getContext()
        val task = FutureTask {
            AccessController.doPrivileged(PrivilegedAction {
                jsBoxRunner.processAfterAllModules(someAssertionWasFailed)
            }, context)
        }
        ForkJoinPool.commonPool().execute(task)
        try {
            task.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            task.cancel(true)
            throw AssertionError("JS code execution timed out after $timeoutMillis ms")
        } catch (e: InterruptedException) {
            task.cancel(true)
            throw e
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
}
