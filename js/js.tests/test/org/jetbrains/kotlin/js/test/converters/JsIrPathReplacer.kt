/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.js.test.utils.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isJsFile
import org.jetbrains.kotlin.test.services.isMjsFile
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File
import kotlin.io.invariantSeparatorsPath

private const val PATH_TO_ROOT_TOKEN = "@PATH_TO_ROOT"

class JsIrPathReplacer(testServices: TestServices) : DeclarationTransformer {
    private val replacements = testServices.collectReplacementsMap()

    override fun lower(irFile: IrFile) {
        super.lower(irFile)
        irFile.replaceJsModulePath()
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return null.also {
            declaration.replaceJsModulePath()
        }
    }

    private fun IrAnnotationContainer.replaceJsModulePath() {
        val jsModuleAnnotation = getAnnotation(JsAnnotations.jsModuleFqn) ?: return

        @Suppress("UNCHECKED_CAST")
        val stringLiteral = jsModuleAnnotation.getValueArgument(0) as IrConst<String>
        val pathReplacement = stringLiteral.getReplacement() ?: return

        jsModuleAnnotation.putValueArgument(0, pathReplacement)
    }

    private fun IrConst<String>.getReplacement(): IrConst<String>? {
        return IrConstImpl.string(startOffset, endOffset, type, replacements[value] ?: return null)
    }

    private fun TestServices.collectReplacementsMap(): Map<String, String> {
        return moduleStructure.modules.asSequence()
            .map { module -> module to module.files.filter { it.isJsFile || it.isMjsFile } }
            .filter { (_, files) -> files.isNotEmpty() }
            .flatMap { (module, files) -> files.map { "./${it.relativePath}" to "$PATH_TO_ROOT_TOKEN/${module.getNameFor(it, this)}" } }
            .plus(getAdditionalFiles(this).map { "./${it.name}" to "$PATH_TO_ROOT_TOKEN/${it.name}" })
            .plus(getAdditionalMainFiles(this).map { "./${it.name}" to "$PATH_TO_ROOT_TOKEN/${it.name}" })
            .toMap()
    }

    fun replacePathTokensWithRealPath(content: String, file: File, rootDir: File): String {
        val relativePathToRootDir = rootDir.relativeTo(file.parentFile).invariantSeparatorsPath.ifEmpty { "." }
        return content.replace("from '$PATH_TO_ROOT_TOKEN/(.+?)';${'$'}".toRegex(RegexOption.MULTILINE)) { result ->
            val importPath = result.groups[1]?.value ?: error("Unexpected import path")
            "from '$relativePathToRootDir/$importPath'\n"
        }
    }
}
