/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.providers.SirParentProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.addChild

public class SirParentProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirParentProvider {

    private val createdExtensionsForModule: MutableMap<SirModule, MutableMap<SirEnum, SirExtension>> = mutableMapOf()

    override fun KtDeclarationSymbol.getSirParent(): SirDeclarationContainer = withSirAnalyse(sirSession, ktAnalysisSession) {
        val symbol = this@getSirParent
        val parentSymbol = symbol.getContainingSymbol()

        if (parentSymbol == null) {
            // top level function. -> parent is either extension for package, of plain module in case of <root> package
            val packageFqName = when (symbol) {
                is KtNamedClassOrObjectSymbol -> symbol.classIdIfNonLocal?.packageFqName
                is KtCallableSymbol -> symbol.callableIdIfNonLocal?.packageName
                is KtTypeAliasSymbol -> symbol.classIdIfNonLocal?.packageFqName
                else -> null
            } ?: error("encountered unknown origin: $symbol. This exception should be reworked during KT-65980")

            return@withSirAnalyse if (packageFqName.isRoot) {
                symbol.getContainingModule().sirModule()
            } else {
                val enumAsPackage = packageFqName.sirPackageEnum(symbol.getContainingModule().sirModule())
                val containingModule = symbol.getContainingModule().sirModule()
                val extensionsInModule = createdExtensionsForModule.getOrPut(containingModule) { mutableMapOf() }
                val extensionForPackage = extensionsInModule.getOrPut(enumAsPackage) {
                    containingModule.addChild {
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
            (parentSymbol.sirDeclaration() as? SirDeclarationContainer)
                ?: error("the found declaration is not parent")
        }
    }
}
