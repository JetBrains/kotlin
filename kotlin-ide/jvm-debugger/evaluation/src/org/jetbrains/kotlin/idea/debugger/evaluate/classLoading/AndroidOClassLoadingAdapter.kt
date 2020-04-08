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

package org.jetbrains.kotlin.idea.debugger.evaluate.classLoading

import com.intellij.debugger.engine.JVMNameUtil
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.isDexDebug

class AndroidOClassLoadingAdapter : AbstractAndroidClassLoadingAdapter() {
    override fun isApplicable(context: ExecutionContext, info: ClassLoadingAdapter.Companion.ClassInfoForEvaluator) = with(info) {
        isCompilingEvaluatorPreferred && context.debugProcess.isDexDebug()
    }

    private fun resolveClassLoaderClass(context: ExecutionContext): ClassType? {
        return try {
            val classLoader = context.classLoader
            tryLoadClass(context, "dalvik.system.InMemoryDexClassLoader", classLoader) as? ClassType
        } catch (e: EvaluateException) {
            null
        }
    }

    override fun loadClasses(context: ExecutionContext, classes: Collection<ClassToLoad>): ClassLoaderReference {
        val inMemoryClassLoaderClass = resolveClassLoaderClass(context) ?: error("InMemoryDexClassLoader class not found")
        val constructorMethod = inMemoryClassLoaderClass.concreteMethodByName(
            JVMNameUtil.CONSTRUCTOR_NAME, "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V"
        ) ?: error("Constructor method not found")

        val dexBytes = dex(context, classes) ?: error("Can't dex classes")
        val dexBytesMirror = mirrorOfByteArray(dexBytes, context)
        val dexByteBuffer = wrapToByteBuffer(dexBytesMirror, context)

        val classLoader = context.classLoader
        val args = listOf(dexByteBuffer, classLoader)
        val newClassLoader = context.newInstance(inMemoryClassLoaderClass, constructorMethod, args) as ClassLoaderReference
        context.keepReference(newClassLoader)

        return newClassLoader
    }
}