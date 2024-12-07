/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.sir.lightclasses.nodes.*

public class SirDeclarationFromKtSymbolProvider(
    private val ktModule: KaModule,
    private val sirSession: SirSession,
) : SirDeclarationProvider {

    public override fun KaDeclarationSymbol.sirDeclarations(): List<SirDeclaration> = listOf(
        when (val ktSymbol = this@sirDeclarations) {
            is KaNamedClassSymbol -> {
                createSirClassFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
            is KaConstructorSymbol -> {
                SirInitFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
            is KaNamedFunctionSymbol -> {
                SirFunctionFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
            is KaVariableSymbol -> {
                ktSymbol.toSirVariable()
            }
            is KaTypeAliasSymbol -> {
                SirTypealiasFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
            else -> TODO("encountered unknown symbol type - $ktSymbol. Error system should be reworked KT-65980")
        }
    )

    private fun KaVariableSymbol.toSirVariable(): SirAbstractVariableFromKtSymbol = when (this) {
        is KaEnumEntrySymbol -> SirEnumCaseFromKtSymbol(
            ktSymbol = this,
            ktModule = ktModule,
            sirSession = sirSession,
        )
        else ->
            if (this is KaPropertySymbol
                && isStatic
                && name == StandardNames.ENUM_ENTRIES
            ) {
                SirEnumEntriesStaticPropertyFromKtSymbol(this, ktModule, sirSession)
            } else {
                SirVariableFromKtSymbol(
                    ktSymbol = this@toSirVariable,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
    }
}
