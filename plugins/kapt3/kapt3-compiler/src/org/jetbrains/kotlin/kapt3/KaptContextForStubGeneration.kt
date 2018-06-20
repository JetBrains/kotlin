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

package org.jetbrains.kotlin.kapt3

import com.intellij.openapi.project.Project
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptPaths
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class KaptContextForStubGeneration(
    paths: KaptPaths,
    withJdk: Boolean,
    logger: KaptLogger,
    val project: Project,
    val bindingContext: BindingContext,
    val compiledClasses: List<ClassNode>,
    val origins: Map<Any, JvmDeclarationOrigin>,
    val generationState: GenerationState,
    mapDiagnosticLocations: Boolean,
    processorOptions: Map<String, String>,
    javacOptions: Map<String, String> = emptyMap()
) : KaptContext(paths, withJdk, logger, mapDiagnosticLocations, processorOptions, javacOptions) {
    private val treeMaker = TreeMaker.instance(context)

    override fun preregisterTreeMaker(context: Context) {
        KaptTreeMaker.preRegister(context, this)
    }

    override fun close() {
        (treeMaker as? KaptTreeMaker)?.dispose()
        generationState.destroy()
        super.close()
    }
}