/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirEnumCase
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.sir.lightclasses.nodes.*
import org.jetbrains.sir.lightclasses.utils.SirOperatorTranslationStrategy

public class SirDeclarationFromKtSymbolProvider(
    private val sirSession: SirSession,
) : SirDeclarationProvider {
    public override fun KaDeclarationSymbol.toSir(): SirTranslationResult =
        when (val ktSymbol = this@toSir) {
            is KaNamedClassSymbol -> {
                when (ktSymbol.classKind) {
                    KaClassKind.INTERFACE -> {
                        val protocol = SirProtocolFromKtSymbol(
                            ktSymbol = ktSymbol,
                            sirSession = sirSession,
                        )
                        SirTranslationResult.RegularInterface(
                            declaration = protocol,
                            bridgedImplementation = SirBridgedProtocolImplementationFromKtSymbol(protocol),
                            markerDeclaration = protocol.existentialMarker,
                            existentialExtension = SirExistentialProtocolImplementationFromKtSymbol(protocol),
                            samConverter = protocol.samConverter,
                        )
                    }
                    KaClassKind.ENUM_CLASS -> {
                        createSirEnumFromKtSymbol(ktSymbol, sirSession).let(SirTranslationResult::Enum)
                    }
                    else -> {
                        createSirClassFromKtSymbol(
                            ktSymbol = ktSymbol,
                            sirSession = sirSession,
                        ).let(SirTranslationResult::RegularClass)
                    }
                }
            }
            is KaConstructorSymbol -> {
                SirInitFromKtSymbol(
                    ktSymbol = ktSymbol,
                    sirSession = sirSession,
                ).let(SirTranslationResult::Constructor)
            }
            is KaNamedFunctionSymbol -> {
                SirOperatorTranslationStrategy(ktSymbol)?.translate(sirSession)
                    ?: SirFunctionFromKtSymbol(
                        ktSymbol = ktSymbol,
                        sirSession = sirSession,
                    ).let(SirTranslationResult::RegularFunction)
            }
            is KaEnumEntrySymbol -> {
                SirTranslationResult.EnumCase(
                    SirEnumCase(
                        ktSymbol.name.asString(), emptyList(),
                        sirSession.withSessions { ktSymbol.getSirParent() as SirEnum }
                    )
                )
            }
            is KaVariableSymbol -> {
                if (ktSymbol is KaPropertySymbol && ktSymbol.isExtension) {
                    ktSymbol.getter?.toSirFunction(ktSymbol)?.let {
                        SirTranslationResult.ExtensionProperty(it, ktSymbol.setter?.toSirFunction(ktSymbol))
                    } ?: SirTranslationResult.Untranslatable(KotlinSource(ktSymbol))
                } else {
                    ktSymbol.toSirVariable().let(SirTranslationResult::RegularProperty)
                }
            }
            is KaTypeAliasSymbol -> {
                SirTypealiasFromKtSymbol(
                    ktSymbol = ktSymbol,
                    sirSession = sirSession,
                ).let(SirTranslationResult::TypeAlias)
            }
            else -> TODO("encountered unknown symbol type - $ktSymbol. Error system should be reworked KT-65980")
        }

    private fun KaPropertyAccessorSymbol.toSirFunction(ktPropertySymbol: KaPropertySymbol): SirFunction = SirFunctionFromKtPropertySymbol(
        ktPropertySymbol = ktPropertySymbol,
        ktSymbol = this,
        sirSession = sirSession,
    )

    private fun KaVariableSymbol.toSirVariable(): SirAbstractVariableFromKtSymbol = when (this) {
        is KaEnumEntrySymbol -> SirEnumCaseFromKtSymbol(
            ktSymbol = this,
            sirSession = sirSession,
        )
        else ->
            if (this is KaPropertySymbol
                && isStatic
                && name == StandardNames.ENUM_ENTRIES
            ) {
                SirEnumEntriesStaticPropertyFromKtSymbol(this, sirSession)
            } else {
                SirVariableFromKtSymbol(
                    ktSymbol = this@toSirVariable,
                    sirSession = sirSession,
                )
            }
    }
}
