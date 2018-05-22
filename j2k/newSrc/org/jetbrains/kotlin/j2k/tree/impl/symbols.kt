/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKField
import org.jetbrains.kotlin.j2k.tree.JKMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

interface JKSymbol {
    val target: Any
    val declaredIn: JKSymbol
}

interface JKClassSymbol : JKSymbol {
    val fqName: String?
}

interface JKMethodSymbol : JKSymbol {
    val fqName: String
}

interface JKFieldSymbol : JKSymbol {
    val fqName: String
}

class JKUniverseClassSymbol(override val target: JKClass) : JKClassSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value
}

class JKMultiverseClassSymbol(override val target: PsiClass) : JKClassSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String?
        get() = target.qualifiedName
}

class JKMultiverseKtClassSymbol(override val target: KtClassOrObject): JKClassSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented")
    override val fqName: String?
        get() = target.fqName?.asString()

}

class JKUniverseMethodSymbol(override val target: JKMethod) : JKMethodSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value
}

class JKMultiverseMethodSymbol(override val target: PsiMethod) : JKMethodSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name

}

class JKUniverseFieldSymbol(override val target: JKField) : JKFieldSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name.value
}

class JKMultiverseFieldSymbol(override val target: PsiField) : JKFieldSymbol {
    override val declaredIn: JKSymbol
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val fqName: String
        get() = target.name

}