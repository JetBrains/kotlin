/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.conversions.parentOfType
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKField
import org.jetbrains.kotlin.j2k.tree.JKMethod
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction

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
}

interface JKFieldSymbol : JKNamedSymbol {
    override val fqName: String
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

class JKUniverseMethodSymbol(private val symbolProvider: JKSymbolProvider) : JKMethodSymbol, JKUniverseSymbol<JKMethod> {
    override val name: String
        get() = target.name.value
    override lateinit var target: JKMethod
    override val declaredIn: JKSymbol?
        get() = target.parentOfType<JKClass>()?.let { symbolProvider.provideUniverseSymbol(it) }
    override val fqName: String
        get() = target.name.value // TODO("Fix this")
}

class JKMultiverseMethodSymbol(override val target: PsiMethod, private val symbolProvider: JKSymbolProvider) : JKMethodSymbol {
    override val name: String
        get() = target.name
    override val declaredIn: JKSymbol?
        get() = target.containingClass?.let { symbolProvider.provideDirectSymbol(it) }
    override val fqName: String
        get() = target.name // TODO("Fix this")
}

class JKMultiverseFunctionSymbol(override val target: KtNamedFunction) : JKMethodSymbol {
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name!! // TODO("Fix this")
}

class JKUniverseFieldSymbol : JKFieldSymbol, JKUniverseSymbol<JKField> {
    override val name: String
        get() = target.name.value
    override lateinit var target: JKField
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value // TODO("Fix this")
}

class JKMultiverseFieldSymbol(override val target: PsiField) : JKFieldSymbol {
    override val name: String
        get() = target.name
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name // TODO("Fix this")
}

class JKUnresolvedField(override val target: String) : JKFieldSymbol {
    constructor(target: PsiReference) : this(target.canonicalText)

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target

    override val name: String
        get() = target
}

class JKUnresolvedMethod(override val target: String) : JKMethodSymbol {
    constructor(target: PsiReference) : this(target.canonicalText)

    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String = target

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
