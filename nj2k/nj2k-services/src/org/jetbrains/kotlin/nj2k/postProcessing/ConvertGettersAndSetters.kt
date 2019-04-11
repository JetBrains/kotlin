/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.nj2k.NewJ2kPostProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

class ConvertGettersAndSetters : NewJ2kPostProcessing {
    override val writeActionNeeded: Boolean = true

    data class PropertyData(
        var name: String,
        var getter: Accessor? = null,
        var setter: Accessor? = null
    )

    data class PropertyDataExtended(
        val name: String,
        val getter: Accessor,
        val setter: Accessor?,
        val needOverride: Boolean,
        val target: KtProperty,
        val visibility: KtModifierKeywordToken
    )

    data class Accessor(
        val name: String,
        val target: KtProperty?,
        val ktFunction: KtFunction,
        val isPure: Boolean,
        val isFromCurrentDeclaration: Boolean
    )

    private fun KtExpression.statements() =
        if (this is KtBlockExpression) statements
        else listOf(this)


    private fun KtFunction.getGetterTarget(): KtProperty? =
        collectDescendantsOfType<KtReturnExpression>()
            .singleOrNull()
            ?.returnedExpression
            ?.unpackedReferenceToProperty()
            ?.takeIf {
                it.type() == this.type()!!
            }


    private fun KtFunction.asGetter(klass: KtClassOrObject): Accessor? {
        if (valueParameters.isNotEmpty()) return null
        if (typeParameters.isNotEmpty()) return null
        val name = getterName() ?: return null
        val target = getGetterTarget()
        val isPure = target != null
                && bodyExpression?.statements()?.size == 1

        return Accessor(
            name,
            target,
            this,
            isPure,
            isFromCurrentDeclaration = containingClassOrObject == klass
        )
    }

    private inline fun <reified D : MemberDescriptor, reified P : KtElement> KtClassOrObject.declarations(): List<P> =
        (analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as ClassDescriptor)
            .unsubstitutedMemberScope.getContributedDescriptors { true }
            .filterIsInstance<D>()
            .mapNotNull { it.findPsi() as? P }


    private fun KtFunction.isProcedure() =
        bodyExpression is KtBlockExpression? && !hasDeclaredReturnType()
                || getReturnTypeReference()?.typeElement?.text == "Unit"

    private fun KtFunction.asSetter(klass: KtClassOrObject): Accessor? {
        if (typeParameters.isNotEmpty()) return null
        if (valueParameters.size != 1) return null
        if (!isProcedure()) return null
        val name = setterName() ?: return null

        val target = bodyExpression
            ?.statements()
            ?.singleOrNull()
            ?.let {
                if (it is KtBinaryExpression) {
                    if (it.operationToken != KtTokens.EQ) return@let null
                    val right = it.right as? KtNameReferenceExpression ?: return@let null
                    if (right.resolve() != valueParameters.single()) return@let null
                    it.left?.unpackedReferenceToProperty()
                } else null
            }?.takeIf {
                it.type() == valueParameters.single().type()
            }
        return Accessor(
            name,
            target,
            this,
            isPure = target != null || bodyExpression?.statements()?.isEmpty() == true,
            isFromCurrentDeclaration = containingClassOrObject == klass
        )
    }


    private fun KtFunction.getterName() =
        name?.takeIf { JvmAbi.isGetterName(it) }
            ?.removePrefix("get")
            ?.takeIf {
                it.isNotEmpty() && it.first().isUpperCase()
                        || it.startsWith("is") && it.length > 2 && it[2].isUpperCase()
            }?.decapitalize()
            ?.escaped()

    private fun KtFunction.setterName() =
        name?.takeIf { JvmAbi.isSetterName(it) }
            ?.removePrefix("set")
            ?.takeIf { it.first().isUpperCase() }
            ?.decapitalize()
            ?.escaped()

    private fun KtClassOrObject.collectGetterAndSettersPairs(): Sequence<PropertyData> {
        val properties = mutableMapOf<String, PropertyData>()
        val functions = declarations<SimpleFunctionDescriptor, KtFunction>()

        for (declaration in functions) {
            declaration.asGetter(this)
                ?.also { getter ->
                    properties.getOrPut(getter.name.removePrefix("is").decapitalize()) {
                        PropertyData(getter.name)
                    }.also { it.getter = getter }
                }

            declaration.asSetter(this)
                ?.also { setter ->
                    properties.getOrPut(setter.name) {
                        PropertyData(setter.name)
                    }.also { it.setter = setter }
                }
        }
        return properties.values.asSequence()
    }

    private fun List<KtModifierListOwner>.maxVisibility(): KtModifierKeywordToken =
        map { it.visibilityModifierTypeOrDefault() }
            .maxBy {
                when (it) {
                    KtTokens.PUBLIC_KEYWORD -> 4
                    KtTokens.INTERNAL_KEYWORD -> 3
                    KtTokens.PROTECTED_KEYWORD -> 2
                    KtTokens.PRIVATE_KEYWORD -> 1
                    else -> 0
                }
            } ?: KtTokens.DEFAULT_VISIBILITY_KEYWORD

    private fun generatePropertiesData(klass: KtClassOrObject): List<PropertyDataExtended> {
        val propertyByName = klass
            .declarations<PropertyDescriptor, KtProperty>()
            .map { it.name!! to it }
            .toMap()
        val factory = KtPsiFactory(klass)


        return klass.collectGetterAndSettersPairs().mapNotNull { (name, getter, setter) ->
            if (getter == null) return@mapNotNull null

            val referencedProperty = propertyByName[name]
            if (referencedProperty != null
                && getter.target != referencedProperty
                && referencedProperty.containingClassOrObject == klass
            ) return@mapNotNull null

            val setterAndGetterHaveTheSameTypes =
                setter != null
                        && setter.ktFunction.valueParameters.single().type() == getter.ktFunction.type()

            if (klass.isInterfaceClass()
                && setter != null
                && !setterAndGetterHaveTheSameTypes
            ) return@mapNotNull null

            val needOverride =
                getter.target?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true
                        || getter.ktFunction.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                        || setter?.ktFunction?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true

            val realTarget =
                getter.target?.takeIf {
                    !it.hasUsagesOutsideOf(klass.containingKtFile, listOfNotNull(getter.ktFunction, setter?.ktFunction))
                            || getter.isPure && setter?.isPure != false
                }
                    ?.takeIf {
                        getter.isFromCurrentDeclaration
                    }

            if (getter.target?.visibilityModifierType() == KtTokens.PUBLIC_KEYWORD
                && (!getter.isPure || setter?.isPure == false)
            ) return@mapNotNull null

            if (realTarget == null
                && referencedProperty != null
                && referencedProperty.containingClassOrObject == klass
            ) return@mapNotNull null

            val target = realTarget
//                ?.takeIf {
//                    getter.isFromCurrentDeclaration
//                }
                ?: factory.createProperty(
                    name,
                    getter.ktFunction.getReturnTypeReference()?.text,
                    isVar = true
                )

            val visibility =
                listOfNotNull(setter?.ktFunction, getter.ktFunction, target).maxVisibility()



            PropertyDataExtended(
                name,
                getter,
                setter?.takeIf { setterAndGetterHaveTheSameTypes },
                needOverride,
                target,
                visibility
            )
        }.toList()
    }

    private fun KtExpression.isReferenceToThis() =
        when (this) {
            is KtThisExpression -> instanceReference.resolve() == parentOfType<KtClassOrObject>()
            is KtReferenceExpression -> resolve() == parentOfType<KtClassOrObject>()
            else -> false
        }

    private fun <T : KtExpression> T.withReplacedExpressionInBody(
        from: KtElement,
        to: KtExpression,
        replaceOnlyWriteUsages: Boolean
    ): T = also {
        from.usages()
            .map { it.element }
            .filter { it.isInsideOf(listOf(this)) }
            .forEach { reference ->
                val parent = reference.parent
                val referenceExpression = when {
                    parent is KtQualifiedExpression
                            && parent.receiverExpression.isReferenceToThis() ->
                        parent
                    else -> reference
                }
                if (!replaceOnlyWriteUsages
                    || (referenceExpression.parent as? KtExpression)?.asAssignment()?.left == referenceExpression
                ) {
                    referenceExpression.replace(to)
                }
            }
    }


    private fun KtProperty.addGetter(getterFunction: Accessor, target: KtProperty): KtPropertyAccessor {
        val factory = KtPsiFactory(this)
        val newBody =
            if (getterFunction.isPure && getterFunction.name == getterFunction.target?.name) null
            else {
                getterFunction.ktFunction.bodyExpression
                    ?.withReplacedExpressionInBody(target, factory.createExpression("field"), replaceOnlyWriteUsages = false)
            }
        add(factory.createGetter(newBody))
        getterFunction.ktFunction.modifierList?.also {
            getter!!.addModifiers(it)
        }
        return getter!!
    }


    private fun KtPropertyAccessor.addModifiers(newModifiers: KtModifierList) {
        setModifierList(newModifiers)
        removeModifier(KtTokens.OVERRIDE_KEYWORD)
        removeModifier(KtTokens.FINAL_KEYWORD)
        removeModifier(KtTokens.ABSTRACT_KEYWORD)
    }


    private fun KtProperty.addSetter(setterInfo: Accessor): KtPropertyAccessor {
        val factory = KtPsiFactory(this)
        val newBody =
            if (setterInfo.isPure && setterInfo.name == setterInfo.target?.name) null
            else {
                setterInfo.ktFunction.bodyExpression?.withReplacedExpressionInBody(this, factory.createExpression("field"), true)
            }
        add(factory.createSetter(newBody, setterInfo.ktFunction.valueParameters.single().name!!))
        setterInfo.ktFunction.modifierList?.also {
            setter!!.addModifiers(it)
        }
        return setter!!
    }

    private fun KtElement.forAllUsages(action: (KtElement) -> Unit) {
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile))
            .forEach { action(it.element as KtElement) }
    }

    private fun KtElement.usages() =
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile))


    private fun KtProperty.addGetterAndChangeAllUsages(getter: Accessor, name: String) =
        addGetter(getter, this).also {
            val factory = KtPsiFactory(this)
            getter.ktFunction.forAllUsages { usage ->
                usage.parentOfType<KtCallExpression>()!!.replace(factory.createExpression(name))
            }
        }

    private fun KtProperty.addSetterAndChangeAllUsages(setter: Accessor) =
        addSetter(setter).also {
            val factory = KtPsiFactory(this)
            setter.ktFunction.forAllUsages { usage ->
                val callExpression = usage.parentOfType<KtCallExpression>()!!
                val qualifier = callExpression.getQualifiedExpressionForSelector()
                val newValue = callExpression.valueArguments.single()
                if (qualifier != null) {
                    qualifier.replace(factory.createExpression("${qualifier.receiverExpression.text}.$name = ${newValue.text}"))
                } else {
                    callExpression.replace(factory.createExpression("$name = ${newValue.text}"))
                }
            }
        }

    private fun KtProperty.addDefaultSetter() =
        KtPsiFactory(this).createSetter(null, "value").let { setter ->
            this.add(setter) as KtPropertyAccessor
        }

    override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
        if (element !is KtClassOrObject) return null
        return {
            val factory = KtPsiFactory(element)
            for ((name,
                getterAccessor,
                setterAccessor,
                needOverride,
                target,
                visibility
            ) in generatePropertiesData(element)) {
                if (target.name != name) {//TODO use refactoring
                    target.forAllUsages { usage ->
                        val parent = (usage.parent as? KtExpression)
                            ?.asAssignment()
                            ?.takeIf { it.left == usage }

                        val expression =
                            if (parent != null) factory.createExpression("this.$name")
                            else factory.createExpression(name)

                        usage.replace(expression)
                    }
                    target.setName(name)
                }
                target.addGetterAndChangeAllUsages(getterAccessor, name).also { getter ->
                    getter.setVisibility(visibility)
                }

                val propertySetter =
                    setterAccessor
                        ?.takeIf { it.isFromCurrentDeclaration }
                        ?.let { target.addSetterAndChangeAllUsages(it) }
                        ?: if (target.isVar
                            && (target.visibilityModifierTypeOrDefault() == KtTokens.PRIVATE_KEYWORD
                                    || target.visibilityModifierTypeOrDefault() == KtTokens.PROTECTED_KEYWORD)
                        ) target.addDefaultSetter().also { it.setVisibility(target.visibilityModifierTypeOrDefault()) }
                        else null

                target.setVisibility(visibility)
                val isVar = propertySetter != null

                if (target.isVar != isVar) {
                    target.valOrVarKeyword.replace(if (isVar) factory.createVarKeyword() else factory.createValKeyword())
                }

                if (target.parent?.isPhysical != true) {
                    when {
                        getterAccessor.isFromCurrentDeclaration ->
                            element.addDeclarationAfter(target, getterAccessor.ktFunction)
                        setterAccessor?.isFromCurrentDeclaration == true ->
                            element.addDeclarationAfter(target, setterAccessor.ktFunction)
                    }
                }
                if (getterAccessor.isFromCurrentDeclaration) {
                    getterAccessor.ktFunction.delete()
                }
                if (setterAccessor?.isFromCurrentDeclaration == true) {
                    setterAccessor.ktFunction.delete()
                }
                if (!target.hasModifier(KtTokens.OVERRIDE_KEYWORD) && needOverride) {
                    target.addModifier(KtTokens.OVERRIDE_KEYWORD)
                }
            }
        }
    }
}