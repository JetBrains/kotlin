/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.nj2k.conversions.multiResolveFqName
import org.jetbrains.kotlin.nj2k.conversions.resolveFqName
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class JKSymbolProvider {
    val symbolsByFqName = mutableMapOf<String, JKSymbol>()
    val symbolsByPsi = mutableMapOf<PsiElement, JKSymbol>()
    val symbolsByJK = mutableMapOf<JKDeclaration, JKSymbol>()

    private val elementVisitor = ElementVisitor()

    fun preBuildTree(inputElements: List<PsiElement>) {
        inputElements.forEach { it.accept(elementVisitor) }
    }

    private fun symbolForNonKotlinElement(psi: PsiElement) =
        when (psi) {
            is KtEnumEntry -> JKMultiverseKtEnumEntrySymbol(psi, this)
            is PsiClass -> JKMultiverseClassSymbol(psi, this)
            is KtClassOrObject -> JKMultiverseKtClassSymbol(psi, this)
            is PsiMethod -> JKMultiverseMethodSymbol(psi, this)
            is PsiField -> JKMultiverseFieldSymbol(psi, this)
            is KtFunction -> JKMultiverseFunctionSymbol(psi, this)
            is KtProperty -> JKMultiversePropertySymbol(psi, this)
            is KtParameter -> JKMultiversePropertySymbol(psi, this)
            is PsiParameter -> JKMultiverseFieldSymbol(psi, this)
            is PsiLocalVariable -> JKMultiverseFieldSymbol(psi, this)
            is PsiPackage -> JKMultiversePackageSymbol(psi)
            else -> TODO(psi::class.toString())
        }

    fun provideDirectSymbol(psi: PsiElement): JKSymbol {
        return symbolsByPsi.getOrPut(psi) {
            if (psi is KtLightDeclaration<*, *>)
                psi.kotlinOrigin
                    ?.let { provideDirectSymbol(it) }
                    ?: symbolForNonKotlinElement(psi)
            else symbolForNonKotlinElement(psi)
        }
    }

    internal inline fun <reified T : JKSymbol> provideSymbol(reference: PsiReference): T {
        val target = reference.resolve()
        if (target != null) return provideDirectSymbol(target) as T
        return (if (isAssignable<T, JKUnresolvedField>()) JKUnresolvedField(
            reference.canonicalText,
            this
        ) else JKUnresolvedMethod(reference)) as T
    }

    fun provideUniverseSymbol(psi: PsiElement, jk: JKDeclaration): JKSymbol = provideUniverseSymbol(psi).also {
        when (it) {
            is JKUniverseClassSymbol -> it.target = jk as JKClass
            is JKUniverseFieldSymbol -> it.target = jk as JKVariable
            is JKUniverseMethodSymbol -> it.target = jk as JKMethod
        }
        symbolsByJK[jk] = it
    }

    fun provideUniverseSymbol(psi: PsiElement): JKSymbol =
        symbolsByPsi.getOrPut(psi) {
            when (psi) {
                is PsiVariable -> JKUniverseFieldSymbol(this)
                is PsiMethod -> JKUniverseMethodSymbol(this)
                is PsiClass -> JKUniverseClassSymbol(this)
                else -> TODO()
            }
        }

    fun transferSymbol(to: JKDeclaration, from: JKDeclaration) = symbolsByJK[from]!!.let {
        it as JKUniverseSymbol<JKTreeElement>
        it.target = to
        symbolsByJK[to] = it
    }

    fun provideUniverseSymbol(jk: JKClass): JKClassSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseClassSymbol(this).also { it.target = jk }
    } as JKClassSymbol

    fun provideUniverseSymbol(jk: JKVariable): JKFieldSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseFieldSymbol(this).also { it.target = jk }
    } as JKFieldSymbol

    fun provideUniverseSymbol(jk: JKMethod): JKMethodSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseMethodSymbol(this).also { it.target = jk }
    } as JKMethodSymbol

    internal inline fun <reified T : JKSymbol> provideByFqName(
        classId: ClassId,
        multiResolve: Boolean = false,
        context: PsiElement = symbolsByPsi.keys.first()
    ): T {
        val fqName = classId.asSingleFqName().asString().replace('/', '.')
        if (fqName in symbolsByFqName) {
            return symbolsByFqName[fqName] as T
        }
        val resolved =
            if (multiResolve) multiResolveFqName(classId, context).firstOrNull()
            else resolveFqName(classId, context)
        val symbol = resolved?.let(::provideDirectSymbol).safeAs<T>()
        return symbol ?: when {
            isAssignable<T, JKUnresolvedMethod>() -> JKUnresolvedMethod(fqName)
            isAssignable<T, JKUnresolvedField>() -> JKUnresolvedField(fqName, this)
            else -> JKUnresolvedClassSymbol(fqName)
        } as T
    }

    @Deprecated("", ReplaceWith("provideByFqName(fqName, true, context)"))
    internal inline fun <reified T : JKSymbol> provideByFqNameMulti(fqName: String, context: PsiElement = symbolsByPsi.keys.first()): T =
        provideByFqName(ClassId.fromString(fqName), true, context)

    internal inline fun <reified T : JKSymbol> provideByFqName(
        fqName: String,
        multiResolve: Boolean = false,
        context: PsiElement = symbolsByPsi.keys.first()
    ): T =
        provideByFqName(ClassId.fromString(fqName), multiResolve, context)

    internal inline fun <reified T : JKSymbol> provideByFqName(
        fqName: FqName,
        multiResolve: Boolean = false,
        context: PsiElement = symbolsByPsi.keys.first()
    ): T =
        provideByFqName(fqName.asString(), multiResolve, context)

    internal inline fun <reified T : JKSymbol> provideByFqName(
        fqName: FqNameUnsafe,
        multiResolve: Boolean = false,
        context: PsiElement = symbolsByPsi.keys.first()
    ): T =
        provideByFqName(fqName.asString(), multiResolve, context)


    private inner class ElementVisitor : JavaElementVisitor() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitClass(aClass: PsiClass) {
            provideUniverseSymbol(aClass)
            aClass.acceptChildren(this)
        }

        override fun visitField(field: PsiField) {
            provideUniverseSymbol(field)
        }

        override fun visitParameter(parameter: PsiParameter) {
            provideUniverseSymbol(parameter)
        }

        override fun visitMethod(method: PsiMethod) {
            provideUniverseSymbol(method)
            method.acceptChildren(this)
        }

        override fun visitEnumConstant(enumConstant: PsiEnumConstant) {
            provideUniverseSymbol(enumConstant)
            enumConstant.acceptChildren(this)
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }

    internal inline fun <reified A, reified B> isAssignable(): Boolean = A::class.java.isAssignableFrom(B::class.java)
}