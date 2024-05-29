/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirParentProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.util.addChild

public class SirParentProviderImpl(
    private val sirSession: SirSession,
    private val packageEnumGenerator: SirEnumGenerator
) : SirParentProvider {

    private val createdExtensionsForModule: MutableMap<SirModule, MutableMap<SirEnum, SirExtension>> = mutableMapOf()

    override fun KaDeclarationSymbol.getSirParent(ktAnalysisSession: KaSession): SirDeclarationContainer {
        val symbol = this@getSirParent
        val parentSymbol = with(ktAnalysisSession) { symbol.getContainingSymbol() }

        return if (parentSymbol == null) {
            // top level function. -> parent is either extension for package, of plain module in case of <root> package
            val packageFqName = when (symbol) {
                is KaNamedClassOrObjectSymbol -> symbol.classId?.packageFqName
                is KaCallableSymbol -> symbol.callableId?.packageName
                is KaTypeAliasSymbol -> symbol.classId?.packageFqName
                else -> null
            } ?: error("encountered unknown origin: $symbol. This exception should be reworked during KT-65980")

            val ktModule = with(ktAnalysisSession) { symbol.getContainingModule() }
            val sirModule = with(sirSession) { ktModule.sirModule() }
            return if (packageFqName.isRoot) {
                sirModule
            } else {
                val enumAsPackage = with(packageEnumGenerator) { packageFqName.sirPackageEnum() }
                val extensionsInModule = createdExtensionsForModule.getOrPut(sirModule) { mutableMapOf() }
                val extensionForPackage = extensionsInModule.getOrPut(enumAsPackage) {
                    sirModule.addChild {
                        buildExtension {
                            origin = enumAsPackage.origin
                            extendedType = SirNominalType(enumAsPackage)
                            visibility = SirVisibility.PUBLIC
                        }
                    }
                }
                extensionForPackage
            }
        } else {
            (with(sirSession) { parentSymbol.sirDeclaration() } as? SirDeclarationContainer)
                ?: error("the found declaration is not parent")
        }
    }
}
