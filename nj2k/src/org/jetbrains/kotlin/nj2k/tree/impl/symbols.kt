/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree.impl

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsNoWrapping
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.conversions.parentOfType
import org.jetbrains.kotlin.nj2k.parentOfType
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface JKSymbol {
    val target: Any
    val declaredIn: JKSymbol?
    val fqName: String
}

interface JKUnresolvedSymbol : JKSymbol

fun JKSymbol.isUnresolved() =
    this is JKUnresolvedSymbol

interface JKNamedSymbol : JKSymbol {
    val name: String
}

abstract class JKUniverseSymbol<T : JKTreeElement> : JKNamedSymbol {
    abstract override var target: T
    protected abstract val symbolProvider: JKSymbolProvider

    override val declaredIn: JKSymbol?
        get() = target.parentOfType<JKDeclaration>()?.let { symbolProvider.symbolsByJK[it] }
    override val fqName: String
        get() {
            val qualifier =
                declaredIn?.fqName
                    ?: target
                        .parentOfType<JKFile>()
                        ?.packageDeclaration
                        ?.packageName
                        ?.value
            return qualifier?.takeIf { it.isNotBlank() }?.let { "$it." }.orEmpty() + name
        }
}

fun JKSymbol.getDisplayName(): String {
    if (this !is JKUniverseSymbol<*>) return fqName
    return generateSequence(declaredIn as? JKUniverseClassSymbol) { symbol ->
        symbol.declaredIn.safeAs<JKUniverseClassSymbol>()?.takeIf { !it.target.hasOtherModifier(OtherModifier.INNER) }
    }.fold(name) { acc, symbol -> "${symbol.name}.$acc" }
}

fun JKSymbol.fqNameToImport(): String? =
    when {
        this is JKClassSymbol && this !is JKUniverseClassSymbol -> fqName
        else -> null
    }

interface JKClassSymbol : JKNamedSymbol


interface JKMethodSymbol : JKNamedSymbol {
    override val fqName: String
    val receiverType: JKType?
    val parameterTypes: List<JKType>?
    val returnType: JKType?
}

val JKMethodSymbol.parameterNames: List<String>?
    get() {
        return when (this) {
            is JKMultiverseFunctionSymbol -> target.valueParameters.map { it.name ?: return null }
            is JKMultiverseMethodSymbol -> target.parameters.map { it.name ?: return null }
            is JKUniverseMethodSymbol -> target.parameters.map { it.name.value }
            is JKUnresolvedSymbol -> null
            else -> null
        }
    }


val JKMethodSymbol.isStatic: Boolean
    get() = when (this) {
        is JKMultiverseFunctionSymbol -> target.parent is KtObjectDeclaration
        is JKMultiverseMethodSymbol -> target.hasModifierProperty(PsiModifier.STATIC)
        is JKUniverseMethodSymbol -> target.parent?.parent?.safeAs<JKClass>()?.classKind == JKClass.ClassKind.COMPANION
        is JKUnresolvedSymbol -> false
        else -> false
    }


fun JKMethodSymbol.parameterTypesWithUnfoldedVarargs(): Sequence<JKType>? {
    val realParameterTypes = parameterTypes ?: return null
    if (realParameterTypes.isEmpty()) return emptySequence()
    val lastArrayType = realParameterTypes.last().arrayInnerType() ?: return realParameterTypes.asSequence()
    return realParameterTypes.dropLast(1).asSequence() + generateSequence { lastArrayType }
}


interface JKFieldSymbol : JKNamedSymbol {
    override val fqName: String
    val fieldType: JKType?
}

class JKUniverseClassSymbol(override val symbolProvider: JKSymbolProvider) : JKClassSymbol, JKUniverseSymbol<JKClass>() {
    override lateinit var target: JKClass
    override val name: String
        get() = target.name.value
}

class JKMultiverseClassSymbol(
    override val target: PsiClass,
    private val symbolProvider: JKSymbolProvider
) : JKClassSymbol {
    override val declaredIn: JKSymbol?
        get() = target.parentOfType<PsiMember>()?.let { symbolProvider.provideDirectSymbol(it) }

    override val fqName: String
        get() = target.getKotlinFqName()?.asString() ?: target.qualifiedName ?: name

    override val name: String
        get() = target.name!!
}

class JKMultiverseKtClassSymbol(
    override val target: KtClassOrObject,
    private val symbolProvider: JKSymbolProvider
) : JKClassSymbol {
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol?
        get() = target.parentOfType<KtDeclaration>()?.let { symbolProvider.provideDirectSymbol(it) }
    override val fqName: String
        get() = target.fqName?.asString() ?: name
}

class JKUniverseMethodSymbol(override val symbolProvider: JKSymbolProvider) : JKMethodSymbol, JKUniverseSymbol<JKMethod>() {
    override val receiverType: JKType?
        get() = (target.parent as? JKClass)?.let {
            JKClassTypeImpl(symbolProvider.provideUniverseSymbol(it), emptyList()/*TODO*/)
        }
    override val parameterTypes: List<JKType>
        get() = target.parameters.map { it.type.type }
    override val returnType: JKType
        get() = target.returnType.type
    override val name: String
        get() = target.name.value

    override lateinit var target: JKMethod
}

class JKMultiverseMethodSymbol(override val target: PsiMethod, private val symbolProvider: JKSymbolProvider) : JKMethodSymbol {
    override val receiverType: JKType?
        get() = target.containingClass?.let {
            JKClassTypeImpl(symbolProvider.provideDirectSymbol(it) as JKClassSymbol, emptyList()/*TODO*/)
        }
    override val parameterTypes: List<JKType>
        get() = target.parameterList.parameters.map { it.type.toJK(symbolProvider) }
    override val returnType: JKType
        get() = target.returnType!!.toJK(symbolProvider)
    override val name: String
        get() = target.name
    override val declaredIn: JKSymbol?
        get() = target.containingClass?.let { symbolProvider.provideDirectSymbol(it) }
    override val fqName: String
        get() = target.getKotlinFqName()?.asString() ?: target.name
}

class JKMultiverseFunctionSymbol(override val target: KtFunction, private val symbolProvider: JKSymbolProvider) : JKMethodSymbol {
    override val receiverType: JKType?
        get() = target.receiverTypeReference?.toJK(symbolProvider)
    @Suppress("UNCHECKED_CAST")
    override val parameterTypes: List<JKType>?
        get() = target.valueParameters.map { parameter ->
            val type = parameter.typeReference?.toJK(symbolProvider)
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
        get() = target.typeReference?.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.fqName?.asString() ?: target.name!!
}

class JKUniverseFieldSymbol(override val symbolProvider: JKSymbolProvider) : JKFieldSymbol, JKUniverseSymbol<JKVariable>() {
    override val fieldType: JKType
        get() = target.type.type
    override val name: String
        get() = target.name.value
    override lateinit var target: JKVariable
}

class JKMultiverseFieldSymbol(override val target: PsiVariable, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType
        get() = target.type.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.getKotlinFqName()?.asString() ?: target.name!!
}

class JKMultiversePropertySymbol(override val target: KtCallableDeclaration, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType?
        get() = target.typeReference?.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.fqName!!.asString()
}

class JKMultiverseKtEnumEntrySymbol(override val target: KtEnumEntry, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType?
        get() = JKClassTypeImpl(
            symbolProvider.provideDirectSymbol(target.containingClass()!!) as JKClassSymbol,
            emptyList(),
            Nullability.NotNull
        )
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.fqName!!.asString()
}

class JKUnresolvedField(override val target: String, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol, JKUnresolvedSymbol {
    override val fieldType: JKType
        get() {
            val resolvedType = (target as? PsiReferenceExpressionImpl)?.type
            if (resolvedType != null) return resolvedType.toJK(symbolProvider)
            return JKClassTypeImpl(symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.nothing.toSafe()), emptyList())
        }
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target
    override val name: String
        get() = target.substringAfterLast('.')
}

class JKUnresolvedMethod(
    override val target: String,
    override val returnType: JKType = JKNoTypeImpl
) : JKMethodSymbol, JKUnresolvedSymbol {
    constructor(target: PsiReference) : this(target.canonicalText)

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target
    override val receiverType: JKType?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val parameterTypes: List<JKType>
        get() = TODO(target) //To change initializer of created properties use File | Settings | File Templates.
    override val name: String
        get() = target.substringAfterLast('.')
}

class JKUnresolvedClassSymbol(override val target: String) : JKClassSymbol, JKUnresolvedSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target

    override val name: String
        get() = target.substringAfterLast('.')
}

interface JKPackageSymbol : JKNamedSymbol {
    override val target: PsiPackage
}

class JKMultiversePackageSymbol(override val target: PsiPackage) : JKPackageSymbol {
    override val declaredIn
        get() = TODO("not implemented")

    override val name: String
        get() = target.name.orEmpty()

    override val fqName: String
        get() = target.qualifiedName
}


fun JKSymbol.deepestFqName(): String? {
    fun Any.deepestFqNameForTarget(): String? =
        when (this) {
            is PsiMethod -> (findDeepestSuperMethods().firstOrNull() ?: this).getKotlinFqName()?.asString()
            is KtNamedFunction -> findDeepestSuperMethodsNoWrapping(this).firstOrNull()?.getKotlinFqName()?.asString()
            is JKMethod -> psi<PsiElement>()?.deepestFqNameForTarget()
            else -> null
        }
    return target.deepestFqNameForTarget() ?: fqName
}