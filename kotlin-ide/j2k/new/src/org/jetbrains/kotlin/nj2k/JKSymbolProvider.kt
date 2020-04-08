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
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.psi.*


class JKSymbolProvider(private val resolver: JKResolver) {
    private val symbolsByFqName = mutableMapOf<String, JKSymbol>()
    val symbolsByPsi = mutableMapOf<PsiElement, JKSymbol>()
    private val symbolsByJK = mutableMapOf<JKDeclaration, JKSymbol>()

    private val elementVisitor = ElementVisitor()
    lateinit var typeFactory: JKTypeFactory

    fun preBuildTree(inputElements: List<PsiElement>) {
        inputElements.forEach { it.accept(elementVisitor) }
    }

    private fun symbolForNonKotlinElement(psi: PsiElement) =
        when (psi) {
            is PsiTypeParameter -> JKMultiverseTypeParameterSymbol(psi, typeFactory)
            is KtTypeParameter -> JKMultiverseKtTypeParameterSymbol(psi, typeFactory)
            is KtEnumEntry -> JKMultiverseKtEnumEntrySymbol(psi, typeFactory)
            is PsiClass -> JKMultiverseClassSymbol(psi, typeFactory)
            is KtClassOrObject -> JKMultiverseKtClassSymbol(psi, typeFactory)
            is PsiMethod -> JKMultiverseMethodSymbol(psi, typeFactory)
            is PsiField -> JKMultiverseFieldSymbol(psi, typeFactory)
            is KtFunction -> JKMultiverseFunctionSymbol(psi, typeFactory)
            is KtProperty -> JKMultiversePropertySymbol(psi, typeFactory)
            is KtParameter -> JKMultiversePropertySymbol(psi, typeFactory)
            is PsiParameter -> JKMultiverseFieldSymbol(psi, typeFactory)
            is PsiLocalVariable -> JKMultiverseFieldSymbol(psi, typeFactory)
            is PsiPackage -> JKMultiversePackageSymbol(psi, typeFactory)
            is KtTypeAlias -> JKTypeAliasKtClassSymbol(psi, typeFactory)
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
            typeFactory
        ) else JKUnresolvedMethod(reference, typeFactory)) as T
    }

    fun provideUniverseSymbol(psi: PsiElement, declaration: JKDeclaration): JKSymbol = provideUniverseSymbol(psi).also { symbol ->
        when (symbol) {
            is JKUniverseClassSymbol -> symbol.target = declaration as JKClass
            is JKUniverseFieldSymbol -> symbol.target = declaration as JKVariable
            is JKUniverseMethodSymbol -> symbol.target = declaration as JKMethod
            is JKUniverseTypeParameterSymbol -> symbol.target = declaration as JKTypeParameter
            is JKUniversePackageSymbol -> symbol.target = declaration as JKPackageDeclaration
        }
        symbolsByJK[declaration] = symbol
    }

    fun provideUniverseSymbol(psi: PsiElement): JKSymbol =
        symbolsByPsi.getOrPut(psi) {
            when (psi) {
                is PsiVariable -> JKUniverseFieldSymbol(typeFactory)
                is PsiMethod -> JKUniverseMethodSymbol(typeFactory)
                is PsiTypeParameter -> JKUniverseTypeParameterSymbol(typeFactory)
                is PsiClass -> JKUniverseClassSymbol(typeFactory)
                is PsiPackageStatement -> JKUniversePackageSymbol(typeFactory)
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
        JKUniverseClassSymbol(typeFactory).also { it.target = jk }
    } as JKClassSymbol

    fun provideUniverseSymbol(jk: JKVariable): JKFieldSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseFieldSymbol(typeFactory).also { it.target = jk }
    } as JKFieldSymbol

    fun provideUniverseSymbol(jk: JKMethod): JKMethodSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseMethodSymbol(typeFactory).also { it.target = jk }
    } as JKMethodSymbol

    fun provideUniverseSymbol(jk: JKTypeParameter): JKTypeParameterSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseTypeParameterSymbol(typeFactory).also { it.target = jk }
    } as JKTypeParameterSymbol

    fun provideUniverseSymbol(jk: JKPackageDeclaration): JKPackageSymbol = symbolsByJK.getOrPut(jk) {
        JKUniversePackageSymbol(typeFactory).also { it.target = jk }
    } as JKPackageSymbol

    fun provideUniverseSymbol(jk: JKDeclaration): JKUniverseSymbol<*>? = when (jk) {
        is JKClass -> provideUniverseSymbol(jk)
        is JKVariable -> provideUniverseSymbol(jk)
        is JKMethod -> provideUniverseSymbol(jk)
        is JKTypeParameter -> provideUniverseSymbol(jk)
        else -> null
    } as? JKUniverseSymbol<*>


    fun provideClassSymbol(fqName: FqName): JKClassSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveClass(fqName)?.let {
                provideDirectSymbol(it) as? JKClassSymbol
            }
        } as? JKClassSymbol ?: JKUnresolvedClassSymbol(fqName.asString(), typeFactory)

    fun provideClassSymbol(fqName: String): JKClassSymbol =
        provideClassSymbol(FqName(fqName.asSafeFqNameString()))

    fun provideClassSymbol(fqName: FqNameUnsafe): JKClassSymbol =
        provideClassSymbol(fqName.toSafe())

    fun provideMethodSymbol(fqName: FqName): JKMethodSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveMethod(fqName)?.let {
                provideDirectSymbol(it) as? JKMethodSymbol
            }
        } as? JKMethodSymbol ?: JKUnresolvedMethod(fqName.asString(), typeFactory)

    fun provideMethodSymbol(fqName: String): JKMethodSymbol =
        provideMethodSymbol(FqName(fqName.asSafeFqNameString()))

    fun provideFieldSymbol(fqName: FqName): JKFieldSymbol =
        symbolsByFqName.getOrPutIfNotNull(fqName.asString()) {
            resolver.resolveField(fqName)?.let {
                provideDirectSymbol(it) as? JKFieldSymbol
            }
        } as? JKFieldSymbol ?: JKUnresolvedField(fqName.asString(), typeFactory)

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

        override fun visitPackageStatement(statement: PsiPackageStatement) {
            provideUniverseSymbol(statement)
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