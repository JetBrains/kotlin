/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTrampolineDeclarationsProvider
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineExtension
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineExtensionVariable
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineFunction
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineVariable
import org.jetbrains.kotlin.sir.providers.utils.containingModule

public class SirTrampolineDeclarationsProviderImpl(
    private val sirSession: SirSession,
    private val targetPackageFqName: FqName?,
) : SirTrampolineDeclarationsProvider {
    private val generatedDeclarations: MutableMap<SirDeclaration, List<SirDeclaration>> = mutableMapOf()

    override fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> = generateDeclarations(this)

    private fun generateDeclarations(declaration: SirDeclaration): List<SirDeclaration> = generatedDeclarations.getOrPut(declaration) {
        // We don't create trampolines for setters if a trampoline extension variable is generated for the getter
        if (declaration is SirSetterFunction && declaration.getter.trampolineDeclarations()
                .any { it is SirTrampolineExtension && it.trampoline is SirTrampolineExtensionVariable }
        ) {
            return@getOrPut emptyList()
        }
        // Always create extension trampolines for top-level declarations
        val parent = declaration.parent
        if (parent is SirModule) {
            return@getOrPut listOfNotNull(declaration.trampolineExtensionDeclaration())
        }
        // Create trampolines for packages that are exported to the root
        if (targetPackageFqName == null) return@getOrPut emptyList()
        val targetPackageEnum = when (declaration) {
            is SirEnum if declaration.isNamespace(targetPackageFqName) -> declaration // avoid recursion
            else -> with(sirSession.enumGenerator) { targetPackageFqName.sirPackageEnum() }
        }
        val shouldExportToRoot = when (parent) {
            is SirEnum -> parent == targetPackageEnum
            is SirExtension -> parent.extendedType == SirNominalType(targetPackageEnum)
            else -> false
        }
        if (!shouldExportToRoot) return@getOrPut emptyList()
        listOfNotNull(declaration.trampolineExtensionDeclaration() ?: declaration.trampolineDeclaration())
    }

    private fun SirDeclaration.trampolineExtensionDeclaration(): SirTrampolineExtension? = when (this) {
        is SirGetterFunction -> SirTrampolineExtension(this)
        is SirFunction -> SirTrampolineExtension(this)
        else -> null
    }?.also { it.parent = this.containingModule() }

    private fun SirDeclaration.trampolineDeclaration(): SirDeclaration? = when (val declaration = this@trampolineDeclaration) {
        is SirScopeDefiningDeclaration -> {
            buildTypealias {
                origin = SirOrigin.Trampoline(declaration)
                visibility = declaration.visibility
                documentation = declaration.documentation
                name = declaration.name
                type = SirNominalType(declaration)
                attributes += declaration.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI }
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
