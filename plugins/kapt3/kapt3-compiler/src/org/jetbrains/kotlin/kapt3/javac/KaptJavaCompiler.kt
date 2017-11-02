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

package org.jetbrains.kotlin.kapt3.javac

import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List as JavacList

class KaptJavaCompiler(context: Context) : JavaCompiler(context) {
    public override fun shouldStop(cs: CompileStates.CompileState) = super.shouldStop(cs)

    fun <T> stopIfErrorOccurred(cs: CompileStates.CompileState, list: JavacList<T>): JavacList<T> {
        return if (shouldStop(cs)) JavacList.nil<T>() else list
    }

    companion object {
        internal fun preRegister(context: Context) {
            context.put(compilerKey, Context.Factory<JavaCompiler>(::KaptJavaCompiler))
        }
    }
}