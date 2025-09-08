/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.containingModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirParentProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.sir.util.addChild

public class SirParentProviderImpl(
    private val sirSession: SirSession,
    private val packageEnumGenerator: SirEnumGenerator
) : SirParentProvider {

    private val createdExtensionsForModule: MutableMap<SirModule, MutableMap<SirEnum, SirExtension>> = mutableMapOf()

    override fun KaDeclarationSymbol.getOriginalSirParent(): SirElement = sirSession.withSessions {
        this@getOriginalSirParent.containingDeclaration?.toSir()?.primaryDeclaration
            ?: this@getOriginalSirParent.containingModule.sirModule()
    }

    override fun KaDeclarationSymbol.getSirParent(): SirDeclarationContainer = sirSession.withSessions {
        val symbol = this@getSirParent
        val parentSymbol = symbol.containingDeclaration

        return@withSessions if (parentSymbol == null) {
            // top level function. -> parent is either extension for package, of plain module in case of <root> package
            val packageFqName = when (symbol) {
                is KaNamedClassSymbol -> symbol.classId?.packageFqName
                is KaCallableSymbol -> symbol.callableId?.packageName
                is KaTypeAliasSymbol -> symbol.classId?.packageFqName
                else -> null
            } ?: error("encountered unknown origin: $symbol. This exception should be reworked during KT-65980")

            val ktModule = symbol.containingModule
            val sirModule = with(sirSession) { ktModule.sirModule() }
            return@withSessions if (packageFqName.isRoot || sirModule is SirPlatformModule) {
                sirModule
            } else {
                val enumAsPackage = with(packageEnumGenerator) { packageFqName.sirPackageEnum() }
                val extensionsInModule = createdExtensionsForModule.getOrPut(sirModule) { mutableMapOf() }
                val extensionForPackage = extensionsInModule.getOrPut(enumAsPackage) {
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
                extensionForPackage
            }
        } else {
            if (symbol is KaClassSymbol && parentSymbol is KaNamedClassSymbol && parentSymbol.classKind == KaClassKind.INTERFACE) {
                parentSymbol.containingModule.sirModule()
            } else {
                parentSymbol.toSir().primaryDeclaration as? SirDeclarationContainer
                    ?: error("parent declaration does not produce suitable SIR")
            }
        }
    }
}
