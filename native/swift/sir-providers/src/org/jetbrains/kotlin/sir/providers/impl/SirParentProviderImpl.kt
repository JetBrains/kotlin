/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.util.SirPlatformLikeModule
import org.jetbrains.kotlin.sir.util.addChild

public class SirParentProviderImpl(
    private val sirSession: SirSession,
    private val packageEnumGenerator: SirEnumGenerator?,
) : SirParentProvider {

    private val createdExtensionsForModule: MutableMap<SirModule, MutableMap<SirEnum, SirExtension>> = mutableMapOf()

    override fun KaDeclarationSymbol.getOriginalSirParent(): SirElement = sirSession.withSessions {
        this@getOriginalSirParent.containingDeclaration?.toSir()?.primaryDeclaration
            ?: this@getOriginalSirParent.containingModule.sirModule()
    }

    override fun KaDeclarationSymbol.getSirParent(): SirDeclarationContainer = sirSession.withSessions {
        val symbol = this@getSirParent
        val parentSymbol = symbol.containingDeclaration

        if (parentSymbol == null) {
            val ktModule = symbol.containingModule
            val sirModule = with(sirSession) { ktModule.sirModule() }
            if (packageEnumGenerator == null || sirModule is SirPlatformLikeModule) return@withSessions sirModule

            // top level declaration -> parent is either extension for package, or plain module in case of <root> package
            val packageFqName = when (symbol) {
                is KaNamedClassSymbol -> symbol.classId?.packageFqName
                is KaCallableSymbol -> symbol.callableId?.packageName
                is KaTypeAliasSymbol -> symbol.classId?.packageFqName
                else -> null
            } ?: error("encountered unknown origin: $symbol. This exception should be reworked during KT-65980")
            if (packageFqName.isRoot) return@withSessions sirModule

            val enumAsPackage = with(packageEnumGenerator) { packageFqName.sirPackageEnum() }
            val extensionsInModule = createdExtensionsForModule.getOrPut(sirModule) { mutableMapOf() }
            return@withSessions extensionsInModule.getOrPut(enumAsPackage) {
                sirModule.updateImport(
                    SirImport(
                        moduleName = enumAsPackage.containingModule().name,
                        // so the user will have access to the Fully Qualified Name for declaration without importing additional modules
                        mode = SirImport.Mode.Exported,
                    )
                )
                sirModule.addChild {
                    buildExtension {
                        origin = enumAsPackage.origin
                        extendedType = SirNominalType(enumAsPackage)
                        visibility = SirVisibility.PUBLIC
                    }
                }
            }
        }
        if (symbol is KaClassSymbol && parentSymbol is KaNamedClassSymbol && parentSymbol.classKind == KaClassKind.INTERFACE) {
            return@withSessions parentSymbol.containingModule.sirModule()
        }
        parentSymbol.toSir().primaryDeclaration as? SirDeclarationContainer
            ?: error("parent declaration does not produce suitable SIR")
    }
}
