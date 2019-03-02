/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.javac

import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List as JavacList

class KaptJavaCompiler(context: Context) : JavaCompiler(context) {
    public override fun shouldStop(cs: CompileStates.CompileState) = super.shouldStop(cs)

    fun <T> stopIfErrorOccurred(cs: CompileStates.CompileState, list: JavacList<T>): JavacList<T> {
        return if (shouldStop(cs)) JavacList.nil<T>() else list
    }

    fun getTaskListeners() = this.taskListener

    companion object {
        internal fun preRegister(context: Context) {
            context.put(compilerKey, Context.Factory<JavaCompiler>(::KaptJavaCompiler))
        }
    }
}