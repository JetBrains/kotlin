/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import com.intellij.util.reverse
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.conversions.parentOfType
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.types.KotlinType

interface JKSymbol {
    val target: Any
    val declaredIn: JKSymbol?
    val fqName: String?
}

interface JKNamedSymbol : JKSymbol {
    val name: String
}

interface JKUniverseSymbol<T: JKTreeElement> : JKSymbol {
    override var target: T
}

interface JKClassSymbol : JKNamedSymbol


interface JKMethodSymbol : JKNamedSymbol {
    override val fqName: String
    val receiverType: JKType?
    val parameterTypes: List<JKType>
    val returnType: JKType
}

interface JKFieldSymbol : JKNamedSymbol {
    override val fqName: String
    val fieldType: JKType
}

class JKUniverseClassSymbol : JKClassSymbol, JKUniverseSymbol<JKClass> {
    override lateinit var target: JKClass
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value // TODO("Fix this")
    override val name: String
        get() = target.name.value
}

class JKMultiverseClassSymbol(override val target: PsiClass) : JKClassSymbol {

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String?
        get() = target.qualifiedName

    override val name: String
        get() = target.name!!
}

class JKMultiverseKtClassSymbol(override val target: KtClassOrObject) : JKClassSymbol {
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented")
    override val fqName: String?
        get() = target.fqName?.asString()

}

fun JKClassSymbol.displayName() =
    when (this) {
        is JKUniverseClassSymbol ->
            target.psi<PsiClass>()
                ?.let { it.nameWithOuterClasses() }
                ?: name
        is JKMultiverseClassSymbol -> target.nameWithOuterClasses()
        else -> name
    }

fun PsiClass.nameWithOuterClasses() =
    generateSequence(this) { it.containingClass }
        .toList()
        .reversed()
        .joinToString(separator = ".") { it.name!! }

class JKUniverseMethodSymbol(private val symbolProvider: JKSymbolProvider) : JKMethodSymbol, JKUniverseSymbol<JKMethod> {
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
    override val declaredIn: JKSymbol?
        get() = target.parentOfType<JKClass>()?.let { symbolProvider.provideUniverseSymbol(it) }
    override val fqName: String
        get() = target.name.value // TODO("Fix this")
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
        get() = target.name // TODO("Fix this")
}

class JKMultiverseFunctionSymbol(override val target: KtNamedFunction, private val symbolProvider: JKSymbolProvider) : JKMethodSymbol {
    override val receiverType: JKType?
        get() = target.receiverTypeReference?.typeElement?.toJK(symbolProvider)
    override val parameterTypes: List<JKType>
        get() = target.valueParameters.map { it.typeReference!!.typeElement!!.toJK(symbolProvider) }
    override val returnType: JKType
        get() = target.typeReference!!.typeElement!!.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name!! // TODO("Fix this")
}

class JKUniverseFieldSymbol : JKFieldSymbol, JKUniverseSymbol<JKVariable> {
    override val fieldType: JKType
        get() = target.type.type
    override val name: String
        get() = target.name.value
    override lateinit var target: JKVariable
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value // TODO("Fix this")
}

class JKMultiverseFieldSymbol(override val target: PsiVariable, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType
        get() = target.type.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name!! // TODO("Fix this")
}

class JKMultiversePropertySymbol(override val target: KtProperty, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType
        get() = target.typeReference!!.typeElement!!.toJK(symbolProvider)
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name!! // TODO("Fix this")
}

class JKUnresolvedField(override val target: PsiReference, private val symbolProvider: JKSymbolProvider) : JKFieldSymbol {
    override val fieldType: JKType
        get() {
            val resolvedType = (target as? PsiReferenceExpressionImpl)?.type
            if (resolvedType != null) return resolvedType.toJK(symbolProvider)

            val nothingSymbol = (symbolProvider.provideDirectSymbol(
                resolveFqName(ClassId.fromString("kotlin.Nothing"), symbolProvider.symbolsByPsi.keys.first())!!
            ) as JKClassSymbol)
            return JKClassTypeImpl(nothingSymbol, emptyList())
        }
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target.canonicalText
    override val name: String = target.canonicalText
}

class JKUnresolvedMethod(
    override val target: String,
    override val returnType: JKType = JKNoTypeImpl
) : JKMethodSymbol {
    constructor(target: PsiReference) : this(target.canonicalText)

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target
    override val receiverType: JKType?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val parameterTypes: List<JKType>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val name: String
        get() = target
}

class JKUnresolvedClassSymbol(override val target: String) : JKClassSymbol {

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String?
        get() = target

    override val name: String
        get() = target.substringAfterLast('.')
}
