package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

context(KaSession)
internal val KaDeclarationSymbol?.implementsCloneable: Boolean
    get() {
        return (this as? KaClassOrObjectSymbol)?.implementsCloneable ?: false
    }

context(KaSession)
internal val KaClassOrObjectSymbol.implementsCloneable: Boolean
    get() {
        return superTypes.any {
            it.expandedSymbol?.isCloneable ?: false
        }
    }

internal val KaClassOrObjectSymbol.isCloneable: Boolean
    get() {
        return classId?.isCloneable ?: false
    }

internal val ClassId.isCloneable: Boolean
    get() {
        return asSingleFqName() == StandardNames.FqNames.cloneable.toSafe()
    }

context(KaSession)
internal val KaFunctionSymbol.isClone: Boolean
    get() {
        val cloneCallableId = CallableId(StandardClassIds.Cloneable, Name.identifier("clone"))
        if (this.callableId == cloneCallableId) return true

        return this.allOverriddenSymbols.any { it.callableId == cloneCallableId }
    }