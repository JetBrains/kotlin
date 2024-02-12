package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

context(KtAnalysisSession)
internal val KtDeclarationSymbol?.implementsCloneable: Boolean
    get() {
        return (this as? KtClassOrObjectSymbol)?.implementsCloneable ?: false
    }

context(KtAnalysisSession)
internal val KtClassOrObjectSymbol.implementsCloneable: Boolean
    get() {
        return superTypes.any {
            it.expandedClassSymbol?.isCloneable ?: false
        }
    }

internal val KtClassOrObjectSymbol.isCloneable: Boolean
    get() {
        return classIdIfNonLocal?.isCloneable ?: false
    }

internal val ClassId.isCloneable: Boolean
    get() {
        return asSingleFqName() == StandardNames.FqNames.cloneable.toSafe()
    }

context(KtAnalysisSession)
internal val KtFunctionSymbol.isClone: Boolean
    get() {
        val cloneCallableId = CallableId(StandardClassIds.Cloneable, Name.identifier("clone"))
        if (this.callableIdIfNonLocal == cloneCallableId) return true

        return this.getAllOverriddenSymbols().any { it.callableIdIfNonLocal == cloneCallableId }
    }