/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKNoTypeImpl
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class JKMethodSymbol : JKSymbol {
    abstract val receiverType: JKType?
    abstract val parameterTypes: List<JKType>?
    abstract val returnType: JKType?
}

class JKUniverseMethodSymbol(override val typeFactory: JKTypeFactory) : JKMethodSymbol(), JKUniverseSymbol<JKMethod> {
    override val receiverType: JKType?
        get() = target.parent.safeAs<JKClass>()?.let {
            JKClassTypeImpl(symbolProvider.provideUniverseSymbol(it), emptyList()/*TODO*/)
        }
    override val parameterTypes: List<JKType>
        get() = target.parameters.map { it.type.type }
    override val returnType: JKType
        get() = target.returnType.type

    override lateinit var target: JKMethod
}

class JKMultiverseMethodSymbol(
    override val target: PsiMethod,
    override val typeFactory: JKTypeFactory
) : JKMethodSymbol(), JKMultiverseSymbol<PsiMethod> {
    override val receiverType: JKType?
        get() = target.containingClass?.let {
            JKClassTypeImpl(symbolProvider.provideDirectSymbol(it) as JKClassSymbol, emptyList()/*TODO*/)
        }
    override val parameterTypes: List<JKType>
        get() = target.parameterList.parameters.map { typeFactory.fromPsiType(it.type) }
    override val returnType: JKType
        get() = target.returnType?.let { typeFactory.fromPsiType(it) } // null for constructor call
            ?: symbolProvider.provideClassSymbol(target.getKotlinFqName()!!).asType(Nullability.NotNull)

    override val fqName: String
        get() {
            val kotlinFqName = super.fqName
            return if (target is KtLightMethod) "$kotlinFqName.$name" else kotlinFqName
        }
}

class JKMultiverseFunctionSymbol(
    override val target: KtFunction,
    override val typeFactory: JKTypeFactory
) : JKMethodSymbol(), JKMultiverseKtSymbol<KtFunction> {
    override val receiverType: JKType?
        get() = target.receiverTypeReference?.toJK(typeFactory)

    @Suppress("UNCHECKED_CAST")
    override val parameterTypes: List<JKType>?
        get() = target.valueParameters.map { parameter ->
            val type = parameter.typeReference?.toJK(typeFactory)
            type?.let {
                if (parameter.isVarArg) {
                    JKClassTypeImpl(
                        symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.array.toSafe()),
                        listOf(it)
                    )
                } else it
            }
        }.takeIf { parameters -> parameters.all { it != null } } as? List<JKType>

    override val returnType: JKType?
        get() = target.typeReference?.toJK(typeFactory)
}

class JKUnresolvedMethod(
    override val target: String,
    override val typeFactory: JKTypeFactory,
    override val returnType: JKType = JKNoTypeImpl
) : JKMethodSymbol(), JKUnresolvedSymbol {
    constructor(target: PsiReference, typeFactory: JKTypeFactory) : this(target.canonicalText, typeFactory)

    override val receiverType: JKType?
        get() = null
    override val parameterTypes: List<JKType>
        get() = emptyList()
}

