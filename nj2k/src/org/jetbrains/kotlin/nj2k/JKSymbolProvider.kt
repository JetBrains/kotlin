/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.nj2k.conversions.JKResolver
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.psi.*


class JKSymbolProvider(project: Project, module: Module, contextElement: PsiElement) {
    private val symbolsByFqName = mutableMapOf<String, JKSymbol>()
    val symbolsByPsi = mutableMapOf<PsiElement, JKSymbol>()
    val symbolsByJK = mutableMapOf<JKDeclaration, JKSymbol>()
    private val resolver = JKResolver(project, module, contextElement)

    private val elementVisitor = ElementVisitor()

    fun preBuildTree(inputElements: List<PsiElement>) {
        inputElements.forEach { it.accept(elementVisitor) }
    }

    private fun symbolForNonKotlinElement(psi: PsiElement) =
        when (psi) {
            is PsiTypeParameter -> JKMultiverseTypeParameterSymbol(psi, this)
            is KtTypeParameter -> JKMultiverseKtTypeParameterSymbol(psi, this)
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
            is PsiPackage -> JKMultiversePackageSymbol(psi, this)
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

    internal inline fun <reified T : JKSymbol> provideSymbolForReference(reference: PsiReference): T {
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
            is JKUniverseTypeParameterSymbol -> it.target = jk as JKTypeParameter
        }
        symbolsByJK[jk] = it
    }

    fun provideUniverseSymbol(psi: PsiElement): JKSymbol =
        symbolsByPsi.getOrPut(psi) {
            when (psi) {
                is PsiVariable -> JKUniverseFieldSymbol(this)
                is PsiMethod -> JKUniverseMethodSymbol(this)
                is PsiTypeParameter -> JKUniverseTypeParameterSymbol(this)
                is PsiClass -> JKUniverseClassSymbol(this)
                else -> TODO()
            }
        }

    fun transferSymbol(to: JKDeclaration, from: JKDeclaration) = symbolsByJK[from]?.also {
        @Suppress("UNCHECKED_CAST")
        it as JKUniverseSymbol<JKDeclaration>
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


    fun provideClassSymbol(fqName: FqName): JKClassSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveClass(fqName)?.let {
                provideDirectSymbol(it) as? JKClassSymbol
            }
        } as? JKClassSymbol ?: JKUnresolvedClassSymbol(fqName.asString())

    fun provideClassSymbol(fqName: String): JKClassSymbol =
        provideClassSymbol(FqName(fqName.asSafeFqNameString()))

    fun provideClassSymbol(fqName: FqNameUnsafe): JKClassSymbol =
        provideClassSymbol(fqName.toSafe())

    fun provideMethodSymbol(fqName: FqName): JKMethodSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveMethod(fqName)?.let {
                provideDirectSymbol(it) as? JKMethodSymbol
            }
        } as? JKMethodSymbol ?: JKUnresolvedMethod(fqName.asString())

    fun provideMethodSymbol(fqName: String): JKMethodSymbol =
        provideMethodSymbol(FqName(fqName.asSafeFqNameString()))

    fun provideFieldSymbol(fqName: FqName): JKFieldSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveField(fqName)?.let {
                provideDirectSymbol(it) as? JKFieldSymbol
            }
        } as? JKFieldSymbol ?: JKUnresolvedField(fqName.asString(), this)

    fun provideFieldSymbol(fqName: String): JKFieldSymbol =
        provideFieldSymbol(FqName(fqName.asSafeFqNameString()))


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

        override fun visitTypeParameter(classParameter: PsiTypeParameter) {
            provideUniverseSymbol(classParameter)
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }

    internal inline fun <reified A, reified B> isAssignable(): Boolean = A::class.java.isAssignableFrom(B::class.java)
}

private inline fun <K, V : Any> MutableMap<K, V>.getOrPutIfNotNull(key: K, defaultValue: () -> V?): V? {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue() ?: return null
        put(key, answer)
        answer
    } else {
        value
    }
}

private fun String.asSafeFqNameString() =
    replace('/', '.')