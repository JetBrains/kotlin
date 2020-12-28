/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.asJava.FirLightClassBase
import org.jetbrains.kotlin.idea.asJava.FirLightClassForSymbol
import org.jetbrains.kotlin.idea.asJava.FirLightField
import org.jetbrains.kotlin.idea.asJava.hasJvmFieldAnnotation
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub

internal class FirLightAnonymousClassForSymbol(
    private val anonymousObjectSymbol: KtAnonymousObjectSymbol,
    manager: PsiManager
) : FirLightClassBase(manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>>, PsiAnonymousClass {

    private val _baseClassType: PsiClassType by lazyPub {
        extendsListTypes.firstOrNull()
            ?: implementsListTypes.firstOrNull()
            ?: PsiType.getJavaLangObject(manager, resolveScope)
    }

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        JavaPsiFacade.getElementFactory(manager.project).createReferenceElementByType(baseClassType)

    override fun getBaseClassType(): PsiClassType = _baseClassType

    private val _extendsList by lazyPub { createInheritanceList(forExtendsList = true, anonymousObjectSymbol.superTypes) }
    private val _implementsList by lazyPub { createInheritanceList(forExtendsList = false, anonymousObjectSymbol.superTypes) }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    override fun getOwnFields(): List<KtLightField> = _ownFields
    override fun getOwnMethods(): List<PsiMethod> = _ownMethods


    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(anonymousObjectSymbol) {
            val callableSymbols = anonymousObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
            createMethods(callableSymbols, result)
        }

        result
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        val result = mutableListOf<KtLightField>()
        val nameGenerator = FirLightField.FieldNameGenerator()

        analyzeWithSymbolAsContext(anonymousObjectSymbol) {
            anonymousObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
                .forEach { propertySymbol ->
                    createField(
                        propertySymbol,
                        nameGenerator,
                        isTopLevel = false,
                        forceStatic = false,
                        takePropertyVisibility = propertySymbol.hasJvmFieldAnnotation(),
                        result
                    )
                }
        }

        result
    }

    private val _ownInnerClasses: List<FirLightClassForSymbol> by lazyPub {
        anonymousObjectSymbol.createInnerClasses(manager)
    }

    override fun getOwnInnerClasses(): List<PsiClass> = _ownInnerClasses

    override fun getScope(): PsiElement? = parent
    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        baseClass?.let { InheritanceImplUtil.isInheritorDeep(this, it, classToByPass) } ?: false

    override val kotlinOrigin: KtClassOrObject? = anonymousObjectSymbol.psi as? KtClassOrObject

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getArgumentList(): PsiExpressionList? = null
    override fun isInQualifiedNew(): Boolean = false
    override fun getName(): String? = null
    override fun getNameIdentifier(): KtLightIdentifier? = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.FINAL
    override fun getContainingClass(): PsiClass? = null
    override fun isDeprecated(): Boolean = false //TODO
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun isInterface() = false
    override fun isAnnotationType() = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getQualifiedName(): String? = null
    override fun isEnum() = false
    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: TODO()
    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = kotlinOrigin?.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = kotlinOrigin?.stub

    override fun isEquivalentTo(another: PsiElement?): Boolean = equals(another) //TODO

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightAnonymousClassForSymbol && anonymousObjectSymbol == other.anonymousObjectSymbol)

    override fun hashCode(): Int = anonymousObjectSymbol.hashCode()

    override fun copy() =
        FirLightAnonymousClassForSymbol(anonymousObjectSymbol, manager)

    override fun isValid(): Boolean = super.isValid() && anonymousObjectSymbol.isValid()

    override fun toString() =
        "${this::class.java.simpleName}:${kotlinOrigin?.getDebugText()}"
}