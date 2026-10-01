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

package org.jetbrains.kotlin.kapt

import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.kapt.base.KaptContext
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.StubGenerationScheme
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt.stubs.KaptIrOrigin
import org.jetbrains.kotlin.kapt.util.FirDeclarationsBySource
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class KaptContextForStubGeneration(
    options: KaptOptions,
    withJdk: Boolean,
    logger: KaptLogger,
    val compiledClasses: List<ClassNode>,
    val origins: Map<Any, KaptIrOrigin>,
    val configuration: CompilerConfiguration,
    val classFileFactory: ClassFileFactory,
    firFiles: List<FirFile>,
    val irBuiltIns: IrBuiltIns,
) : KaptContext(options, withJdk, logger) {
    private val treeMaker = TreeMaker.instance(context)

    val compiledClassByName: Map<String, ClassNode> = compiledClasses.associateBy { it.name!! }

    // FirSession can be null in case of incremental compilation, e.g. if only Java files need reprocessing,
    // or all affected Kotlin sources are removed.
    val firSession: FirSession? = firFiles.firstOrNull()?.moduleData?.session

    private val declarationsBySource = mutableMapOf<FirFile, FirDeclarationsBySource>()

    // FIR metadata for [declaration], including generated declarations that only keep source ranges.
    fun firMetadataOf(declaration: IrDeclaration): FirMetadataSource? {
        ((declaration as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.let { return it }

        val firFile = (declaration.fileParent.metadata as? FirMetadataSource.File)?.fir ?: return null
        return declarationsBySource
            .getOrPut(firFile) { FirDeclarationsBySource.of(firFile) }
            .findInnermostContaining(declaration.startOffset, declaration.endOffset)
    }

    // Source from [firMetadataOf], independent of the parser used.
    fun firSourceOf(declaration: IrDeclaration): KtSourceElement? = firMetadataOf(declaration)?.source

    override fun preregisterTreeMaker(context: Context) {
        KaptTreeMaker.preRegister(context, this)
    }

    override fun close() {
        (treeMaker as? KaptTreeMaker)?.dispose()
        super.close()
    }

    internal fun textGenerationError(message: String): String {
        if (options.stubGenerationScheme == StubGenerationScheme.DIRECT) {
            error(message)
        }
        return "TEXT_GENERATION_ERROR"
    }

    internal inline fun textGenerationRequire(check: Boolean, lazyMessage: () -> String) {
        if (options.stubGenerationScheme == StubGenerationScheme.DIRECT) {
            require(check, lazyMessage)
        }
    }
}
