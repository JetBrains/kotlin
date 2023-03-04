/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat

import org.jetbrains.dukat.astModel.SourceSetModel
import org.jetbrains.dukat.astModel.modifiers.VisibilityModifierModel
import org.jetbrains.dukat.commonLowerings.AddExplicitGettersAndSetters
import org.jetbrains.dukat.idlLowerings.*
import org.jetbrains.dukat.idlParser.parseIDL
import org.jetbrains.dukat.idlReferenceResolver.DirectoryReferencesResolver
import org.jetbrains.dukat.model.commonLowerings.*
import org.jetbrains.dukat.translatorString.compileUnits
import org.jetbrains.dukat.translatorString.translateSourceSet
import org.jetbrains.kotlin.tools.dukat.wasm.convertToWasmModel
import java.io.File

fun main() {
    val outputDirectory = "../../stdlib/wasm/src/org.w3c/"
    val input = "../../stdlib/js/idl/org.w3c.dom.idl"

    val sourceSet = translateIdlToSourceSet(input)
    compileUnits(translateSourceSet(sourceSet), outputDirectory)

    File(outputDirectory).walk().forEach {
        if (it.isFile && it.name.endsWith(".kt")) {
            it.writeText(getHeader() + postProcessIdlBindings(it.readText()))
        }
    }
}

fun translateIdlToSourceSet(fileName: String): SourceSetModel {
    val translationContext = TranslationContext()
    return parseIDL(fileName, DirectoryReferencesResolver())
        .resolvePartials()
        .addConstructors()
        .resolveTypedefs()
        .specifyEventHandlerTypes()
        .specifyDefaultValues()
        .resolveImplementsStatements()
        .resolveMixins()
        .addItemArrayLike()
        .resolveTypes()
        .markAbstractOrOpen()
        .addMissingMembers()
        .addOverloadsForCallbacks()
        .convertToWasmModel()
        .lower(
            ModelContextAwareLowering(translationContext),
            LowerOverrides(translationContext),
            EscapeIdentificators(),
            AddExplicitGettersAndSetters(),
        )
        .lower(ReplaceDynamics())  // Wasm-specific
        .addKDocs()
        .relocateDeclarations()
        .resolveTopLevelVisibility(alwaysPublic())
        .addImportsForUsedPackages()
        .omitStdLib()
}

private fun alwaysPublic(): VisibilityModifierResolver = object : VisibilityModifierResolver {
    override fun resolve(): VisibilityModifierModel = VisibilityModifierModel.PUBLIC
}

// TODO: Backport to dukat
fun postProcessIdlBindings(source: String): String {
    return source
        .replace(
            Regex("( {4}return o as \\w+)"),
            "    @Suppress(\"UNCHECKED_CAST_TO_EXTERNAL_INTERFACE\")\n\$1"
        )
        .replace("js(\"({})\")", "newJsObject()")
}