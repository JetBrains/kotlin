/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.*
import org.jetbrains.uast.java.AbstractJavaUClass
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.kotlin.declarations.UastLightIdentifier

class KotlinUClass private constructor(
        psi: KtLightClass,
        override val uastParent: UElement?
) : AbstractJavaUClass(), PsiClass by psi {

    val ktClass = psi.kotlinOrigin

    override val psi = unwrap<UClass, PsiClass>(psi)

    override fun getOriginalElement(): PsiElement? {
        return super.getOriginalElement()
    }

    override fun getNameIdentifier() = UastLightIdentifier(psi, ktClass)

    override fun getContainingFile(): PsiFile? = ktClass?.containingFile ?: psi.containingFile

    override val annotations: List<UAnnotation>
        get() = ktClass?.annotationEntries?.map { KotlinUAnnotation(it, this) } ?: emptyList()

    override val uastAnchor: UElement
        get() = UIdentifier(psi.nameIdentifier, this)

    override fun getInnerClasses(): Array<UClass> {
        // filter DefaultImpls to avoid processing same methods from original interface multiple times
        // filter Enum entry classes to avoid duplication with PsiEnumConstant initializer class
        return psi.innerClasses.filter {
            it.name != JvmAbi.DEFAULT_IMPLS_CLASS_NAME && !it.isEnumEntryLightClass()
        }.map {
            getLanguagePlugin().convert<UClass>(it, this)
        }.toTypedArray()
    }

    override fun getSuperClass(): UClass? = super.getSuperClass()
    override fun getFields(): Array<UField> = super.getFields()
    override fun getInitializers(): Array<UClassInitializer> = super.getInitializers()

    override fun getMethods(): Array<UMethod> {
        val primaryConstructor = ktClass?.primaryConstructor?.toLightMethods()?.firstOrNull()
        val initBlocks = ktClass?.getAnonymousInitializers() ?: emptyList()

        fun createUMethod(psiMethod: PsiMethod): UMethod {
            return if (psiMethod is KtLightMethod && psiMethod.isConstructor && initBlocks.isNotEmpty()
                    && (primaryConstructor == null || psiMethod == primaryConstructor)) {
                object : KotlinUMethod(psiMethod, this@KotlinUClass) {
                    override val uastBody by lz {
                        val initializers = ktClass?.getAnonymousInitializers() ?: return@lz UastEmptyExpression
                        val containingMethod = this

                        object : UBlockExpression {
                            override val psi: PsiElement?
                                 get() = null

                            override val uastParent: UElement?
                                get() = containingMethod

                            override val annotations: List<UAnnotation>
                                get() = emptyList()

                            override val expressions by lz {
                                initializers.map {
                                    getLanguagePlugin().convertOpt<UExpression>(it.body, this) ?: UastEmptyExpression
                                }
                            }
                        }
                    }
                }
            } else {
                getLanguagePlugin().convert(psiMethod, this)
            }
        }

        fun isDelegatedMethod(psiMethod: PsiMethod) = psiMethod is KtLightMethod && psiMethod.isDelegated

        return psi.methods.asSequence()
                .filterNot(::isDelegatedMethod)
                .map(::createUMethod)
                .toList()
                .toTypedArray()
    }

    private fun PsiClass.isEnumEntryLightClass() = (this as? KtLightClass)?.kotlinOrigin is KtEnumEntry

    companion object {
        fun create(psi: KtLightClass, containingElement: UElement?): UClass {
            return if (psi is PsiAnonymousClass)
                KotlinUAnonymousClass(psi, containingElement)
            else
                KotlinUClass(psi, containingElement)
        }
    }
}

class KotlinUAnonymousClass(
        psi: PsiAnonymousClass,
        override val uastParent: UElement?
) : AbstractJavaUClass(), UAnonymousClass, PsiAnonymousClass by psi {

    override val psi: PsiAnonymousClass = unwrap<UAnonymousClass, PsiAnonymousClass>(psi)

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractJavaUClass>.getOriginalElement()
    }

    override fun getSuperClass(): UClass? = super<AbstractJavaUClass>.getSuperClass()
    override fun getFields(): Array<UField> = super<AbstractJavaUClass>.getFields()
    override fun getMethods(): Array<UMethod> = super<AbstractJavaUClass>.getMethods()
    override fun getInitializers(): Array<UClassInitializer> = super<AbstractJavaUClass>.getInitializers()
    override fun getInnerClasses(): Array<UClass> = super<AbstractJavaUClass>.getInnerClasses()

    override val uastAnchor: UElement?
        get() {
            val ktClassOrObject = (psi.originalElement as? KtLightClass)?.kotlinOrigin as? KtObjectDeclaration ?: return null 
            return UIdentifier(ktClassOrObject.getObjectKeyword(), this)
        }
}