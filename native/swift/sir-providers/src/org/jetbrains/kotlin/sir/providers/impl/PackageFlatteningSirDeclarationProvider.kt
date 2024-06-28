/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTrampolineDeclarationsProvider
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineFunction
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineVariable

public class SirTrampolineDeclarationsProviderImpl(
    private val sirSession: SirSession,
    private val targetPackageFqName: FqName?,
    private val enumGenerator: SirEnumGenerator,
) : SirTrampolineDeclarationsProvider {
    private val generatedDeclarations: MutableMap<SirDeclaration, List<SirDeclaration>> = mutableMapOf()

    override fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> = generateDeclarations(this)

    private fun generateDeclarations(declaration: SirDeclaration): List<SirDeclaration> = generatedDeclarations.getOrPut(declaration) {
        if (targetPackageFqName == null)
            return emptyList()


        with(sirSession) {
            val targetEnum = if (declaration is SirEnum && declaration.isNamespace(targetPackageFqName)) {
                declaration // avoid recursion
            } else {
                with(enumGenerator) { targetPackageFqName.sirPackageEnum() }
            }

            val shouldExportToRoot = when (val parent = declaration.parent) {
                is SirEnum -> parent == targetEnum
                is SirExtension -> parent.extendedType == SirNominalType(targetEnum)
                else -> false
            }

            return listOfNotNull(declaration.takeIf { shouldExportToRoot }?.trampolineDeclaration())
        }
    }

    private fun SirDeclaration.containingModule(): SirModule = when (val parent = parent) {
        is SirModule -> parent
        is SirDeclaration -> parent.containingModule()
    }

    private fun SirDeclaration.trampolineDeclaration(): SirDeclaration? = when (val declaration = this@trampolineDeclaration) {
        is SirNamedDeclaration -> {
            buildTypealias {
                origin = SirOrigin.Trampoline(declaration)
                visibility = declaration.visibility
                documentation = declaration.documentation
                name = declaration.name
                type = SirNominalType(declaration)
            }
        }
        is SirFunction -> SirTrampolineFunction(declaration)
        is SirVariable -> SirTrampolineVariable(declaration)
        else -> null
    }?.also { it.parent = this.containingModule() }
}

private fun SirEnum.isNamespace(fqName: FqName): Boolean = (this.origin as? SirOrigin.Namespace)?.path?.let {
    val path = fqName.pathSegments()
    it.size == path.size && (it zip path).all { it.first == it.second.toString() }
} ?: false
