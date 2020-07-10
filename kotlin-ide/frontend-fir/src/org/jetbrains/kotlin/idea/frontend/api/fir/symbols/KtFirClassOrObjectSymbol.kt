/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.ReadOnlyWeakRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirClassOrObjectSymbol(
    fir: FirRegularClass,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtClassOrObjectSymbol(), KtFirSymbol<FirRegularClass> {
    override val fir: FirRegularClass by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }
    override val name: Name get() = withValidityAssertion { fir.classId.shortClassName }
    override val classId: ClassId get() = withValidityAssertion { fir.classId }

    override val modality: KtSymbolModality
        get() = withValidityAssertion { fir.modality.getSymbolModality() }

    override val typeParameters by cached {
        fir.typeParameters.map { typeParameter ->
            builder.buildTypeParameterSymbol(typeParameter.symbol.fir)
        }
    }

    override val classKind: KtClassKind
        get() = withValidityAssertion {
            when (fir.classKind) {
                ClassKind.INTERFACE -> KtClassKind.INTERFACE
                ClassKind.ENUM_CLASS -> KtClassKind.ENUM_CLASS
                ClassKind.ENUM_ENTRY -> KtClassKind.ENUM_ENTRY
                ClassKind.ANNOTATION_CLASS -> KtClassKind.ANNOTATION_CLASS
                ClassKind.CLASS -> KtClassKind.CLASS
                ClassKind.OBJECT -> if (fir.isCompanion) KtClassKind.COMPANION_OBJECT else KtClassKind.OBJECT
            }
        }
    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when {
                fir.isLocal -> KtSymbolKind.LOCAL
                fir.symbol.classId.isNestedClass -> KtSymbolKind.MEMBER
                else -> KtSymbolKind.TOP_LEVEL
            }
        }

    override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol> {
        val classId = classId
        return symbolPointer { session -> session.symbolProvider.getClassOrObjectSymbolByClassId(classId) }
    }
}