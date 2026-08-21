/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirTrampolineDeclarationsProvider
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineFunction
import org.jetbrains.kotlin.sir.providers.impl.nodes.SirTrampolineVariable
import org.jetbrains.kotlin.sir.providers.utils.containingModule

public class SirTrampolineDeclarationsProviderImpl(
    enumGenerator: SirEnumGenerator,
    private val rootPackageFqNames: Set<FqName>,
) : SirTrampolineDeclarationsProvider {

    private val rootPackageEnums = with(enumGenerator) {
        rootPackageFqNames.map { it.sirPackageEnum() }
    }

    private val generatedDeclarations: MutableMap<SirDeclaration, List<SirDeclaration>> = mutableMapOf()

    override fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> {
        if (rootPackageEnums.isEmpty()) return emptyList()
        return generatedDeclarations.getOrPut(this) {
            generateDeclarations(this)
        }
    }

    private fun generateDeclarations(declaration: SirDeclaration): List<SirDeclaration> {
        val packageEnum = when (val parent = declaration.parent) {
            is SirEnum if parent.origin is SirOrigin.Namespace -> parent
            is SirExtension -> {
                val extendedType = parent.extendedType as? SirNominalType
                val extendedEnum = extendedType?.typeDeclaration as? SirEnum
                extendedEnum?.takeIf { it.origin is SirOrigin.Namespace }
            }
            else -> null
        }
        if (packageEnum == null || !rootPackageEnums.contains(packageEnum)) return emptyList()
        return listOfNotNull(declaration.trampolineDeclaration())
    }

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
