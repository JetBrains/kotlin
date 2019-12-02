/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asGetterName
import org.jetbrains.kotlin.nj2k.asSetterName
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.JKFakeFieldData
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.JKFieldData
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.JKMethodData
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.NewExternalCodeProcessing
import org.jetbrains.kotlin.nj2k.fqNameWithoutCompanions
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.isJavaDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.mapToIndex

class ConvertGettersAndSettersToPropertyProcessing : ElementsBasedPostProcessing() {
    override fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext) {
        val ktElements = elements.filterIsInstance<KtElement>()
        val resolutionFacade = runReadAction {
            KotlinCacheService.getInstance(converterContext.project).getResolutionFacade(ktElements)
        }
        ConvertGettersAndSettersToPropertyStatefulProcessing(
            resolutionFacade,
            ktElements,
            converterContext.externalCodeProcessor
        ).runProcessing()
    }
}

private class ConvertGettersAndSettersToPropertyStatefulProcessing(
    private val resolutionFacade: ResolutionFacade,
    private val elements: List<KtElement>,
    private val externalCodeUpdater: NewExternalCodeProcessing
) {
    private val searcher = JKInMemoryFilesSearcher.create(elements)


    fun runProcessing() {
        val collectingState = CollectingState()
        val classesWithPropertiesData = runReadAction {
            elements
                .descendantsOfType<KtClassOrObject>()
                .sortedByInheritance()
                .map { klass ->
                    klass to klass.collectPropertiesData(collectingState)
                }
        }
        if (classesWithPropertiesData.isEmpty()) return
        runUndoTransparentActionInEdt(inWriteAction = true) {
            for ((klass, propertiesData) in classesWithPropertiesData) {
                convertClass(klass, propertiesData)
            }
        }
    }

    private fun KtNamedFunction.hasOverrides(): Boolean =
        toLightMethods().singleOrNull()?.let { lightMethod ->
            if (OverridingMethodsSearch.search(lightMethod).findFirst() != null) return@let true
            if (elements.size == 1) return@let false
            elements.any { element ->
                OverridingMethodsSearch.search(lightMethod, LocalSearchScope(element), true).findFirst() != null
            }
        } == true

    private fun KtNamedFunction.hasSuperFunction(): Boolean =
        resolveToDescriptorIfAny(resolutionFacade)?.original?.overriddenDescriptors?.isNotEmpty() == true

    private fun KtNamedFunction.hasInvalidSuperDescriptors(): Boolean =
        resolveToDescriptorIfAny(resolutionFacade)
            ?.original
            ?.overriddenDescriptors
            ?.any { it.isJavaDescriptor || it is FunctionDescriptor } == true

    private fun KtExpression.statements() =
        if (this is KtBlockExpression) statements
        else listOf(this)

    private fun KtNamedFunction.asGetter(): Getter? {
        val name = name?.asGetterName() ?: return null
        if (valueParameters.isNotEmpty()) return null
        if (typeParameters.isNotEmpty()) return null
        val singleTimeUsedTarget = bodyExpression
            ?.statements()
            ?.singleOrNull()
            ?.safeAs<KtReturnExpression>()
            ?.returnedExpression
            ?.unpackedReferenceToProperty()
            ?.takeIf {
                it.type() == type() ?: return@takeIf false
            }
        return RealGetter(this, singleTimeUsedTarget, name, singleTimeUsedTarget != null)
    }

    private fun KtNamedFunction.asSetter(): Setter? {
        val name = name?.asSetterName() ?: return null
        if (typeParameters.isNotEmpty()) return null
        if (valueParameters.size != 1) return null
        val descriptor = resolveToDescriptorIfAny() ?: return null
        if (descriptor.returnType?.isUnit() != true) return null
        val singleTimeUsedTarget = bodyExpression
            ?.statements()
            ?.singleOrNull()
            ?.let { expression ->
                if (expression is KtBinaryExpression) {
                    if (expression.operationToken != KtTokens.EQ) return@let null
                    val right = expression.right as? KtNameReferenceExpression ?: return@let null
                    if (right.resolve() != valueParameters.single()) return@let null
                    expression.left?.unpackedReferenceToProperty()
                } else null
            }?.takeIf {
                it.type() == valueParameters.single().type()
            }
        return RealSetter(this, singleTimeUsedTarget, name, singleTimeUsedTarget != null)
    }

    private fun KtDeclaration.asPropertyAccessor(): PropertyInfo? {
        return when (this) {
            is KtProperty -> RealProperty(this, name ?: return null)
            is KtNamedFunction -> asGetter() ?: asSetter()
            else -> null
        }
    }

    private fun KtElement.forAllUsages(action: (KtElement) -> Unit) {
        usages().forEach { action(it.element as KtElement) }
    }

    private fun addGetter(
        getter: Getter,
        property: KtProperty,
        factory: KtPsiFactory,
        isFakeProperty: Boolean
    ): KtPropertyAccessor {
        val body =
            if (isFakeProperty) getter.body
            else getter.body?.withReplacedExpressionInBody(
                property,
                factory.createExpression(KtTokens.FIELD_KEYWORD.value),
                replaceOnlyWriteUsages = false
            )

        val ktGetter = factory.createGetter(body, getter.modifiersText)
        ktGetter.filterModifiers()
        return property.add(ktGetter).cast<KtPropertyAccessor>().also {
            if (getter is RealGetter) {
                getter.function.forAllUsages { usage ->
                    usage.getStrictParentOfType<KtCallExpression>()!!.replace(factory.createExpression(getter.name))
                }
            }
        }
    }

    private fun KtParameter.rename(newName: String) {
        val renamer = RenamePsiElementProcessor.forElement(this)
        val usageInfos =
            renamer.findReferences(this, false).mapNotNull { reference ->
                val element = reference.element
                val isBackingField = element is KtNameReferenceExpression &&
                        element.text == KtTokens.FIELD_KEYWORD.value
                        && element.mainReference.resolve() == this
                        && isAncestor(element)
                if (isBackingField) return@mapNotNull null
                renamer.createUsageInfo(this, reference, reference.element)
            }.toTypedArray()
        renamer.renameElement(this, newName, usageInfos, null)
    }

    private fun createSetter(
        setter: Setter,
        property: KtProperty,
        factory: KtPsiFactory,
        isFakeProperty: Boolean
    ): KtPropertyAccessor {
        if (setter is RealSetter) {
            setter.function.valueParameters.single().rename(setter.parameterName)
        }
        val body =
            if (isFakeProperty) setter.body
            else setter.body?.withReplacedExpressionInBody(
                property,
                factory.createExpression(KtTokens.FIELD_KEYWORD.value),
                true
            )
        val modifiers = setter.modifiersText?.takeIf { it.isNotEmpty() }
            ?: setter.safeAs<RealSetter>()?.function?.visibilityModifierTypeOrDefault()?.value
            ?: property.visibilityModifierTypeOrDefault().value

        val ktSetter = factory.createSetter(
            body,
            setter.parameterName,
            modifiers
        )
        ktSetter.filterModifiers()
        if (setter is RealSetter) {
            setter.function.forAllUsages { usage ->
                val callExpression = usage.getStrictParentOfType<KtCallExpression>() ?: return@forAllUsages
                val qualifier = callExpression.getQualifiedExpressionForSelector()
                val newValue = callExpression.valueArguments.single()
                if (qualifier != null) {
                    qualifier.replace(
                        factory.createExpression("${qualifier.receiverExpression.text}.${setter.name} = ${newValue.text}")
                    )
                } else {
                    callExpression.replace(factory.createExpression("${setter.name} = ${newValue.text}"))
                }
            }
        }
        return ktSetter
    }

    private fun KtPropertyAccessor.filterModifiers() {
        removeModifier(KtTokens.OVERRIDE_KEYWORD)
        removeModifier(KtTokens.FINAL_KEYWORD)
        removeModifier(KtTokens.OPEN_KEYWORD)
    }

    private fun KtExpression.isReferenceToThis() =
        when (this) {
            is KtThisExpression -> instanceReference.resolve() == getStrictParentOfType<KtClassOrObject>()
            is KtReferenceExpression -> resolve() == getStrictParentOfType<KtClassOrObject>()
            else -> false
        }

    private fun KtElement.usages(scope: PsiElement? = null) =
        searcher.search(this, scope)

    private fun <T : KtExpression> T.withReplacedExpressionInBody(
        from: KtElement,
        to: KtExpression,
        replaceOnlyWriteUsages: Boolean
    ): T = also {
        from.usages(this)
            .asSequence()
            .map { it.element }
            .forEach { reference ->
                val parent = reference.parent
                val referenceExpression = when {
                    parent is KtQualifiedExpression && parent.receiverExpression.isReferenceToThis() ->
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


    private fun List<KtClassOrObject>.sortedByInheritance(): List<KtClassOrObject> {
        fun ClassDescriptor.superClassAndSuperInterfaces() =
            getSuperInterfaces() + listOfNotNull(getSuperClassNotAny())

        val sorted = mutableListOf<KtClassOrObject>()
        val visited = Array(size) { false }
        val descriptors = map { it.resolveToDescriptorIfAny(resolutionFacade)!! }
        val descriptorToIndex = descriptors.mapToIndex()
        val outers = descriptors.map { descriptor ->
            descriptor.superClassAndSuperInterfaces().mapNotNull { descriptorToIndex[it] }
        }

        fun dfs(current: Int) {
            visited[current] = true
            for (outer in outers[current]) {
                if (!visited[outer]) {
                    dfs(outer)
                }
            }
            sorted.add(get(current))
        }

        for (index in descriptors.indices) {
            if (!visited[index]) {
                dfs(index)
            }
        }
        return sorted
    }

    private fun causesNameConflictInCurrentDeclarationAndItsParents(name: String, declaration: DeclarationDescriptor?): Boolean =
        when (declaration) {
            is ClassDescriptor -> {
                declaration.unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.VARIABLES) {
                    it.asString() == name
                }.isNotEmpty()
                        || causesNameConflictInCurrentDeclarationAndItsParents(name, declaration.containingDeclaration)
            }
            else -> false
        }

    private fun calculatePropertyType(getterType: KotlinType?, setterType: KotlinType?): KotlinType? = when {
        getterType != null && getterType.isMarkedNullable -> getterType
        setterType != null && setterType.isMarkedNullable -> setterType
        else -> getterType ?: setterType
    }

    private fun calculatePropertyType(getter: KtNamedFunction?, setter: KtNamedFunction?): KotlinType? {
        val getterType = getter?.resolveToDescriptorIfAny(resolutionFacade)?.returnType
        val setterType = setter?.resolveToDescriptorIfAny(resolutionFacade)?.valueParameters?.singleOrNull()?.type
        return calculatePropertyType(getterType, setterType)
    }

    private inline fun <reified A : RealAccessor> A.withTargetSet(property: RealProperty?): A {
        if (property == null) return this
        if (target != null) return this
        return if(property.property.usages(function).any())
            updateTarget(property.property) as A
        else this
    }

    private fun KtClassOrObject.collectPropertiesData(collectingState: CollectingState): List<PropertyData> {
        return declarations
            .asSequence()
            .mapNotNull { it.asPropertyAccessor() }
            .groupBy { it.name.removePrefix("is").decapitalize() }
            .values
            .mapNotNull { group ->
                val realGetter = group.firstIsInstanceOrNull<RealGetter>()
                val realSetter = group.firstIsInstanceOrNull<RealSetter>()?.takeIf { setter ->
                    if (realGetter == null) return@takeIf true
                    val setterType = setter.function.valueParameters.first().type()
                    val getterType = realGetter.function.type()
                    if (setterType == null
                        || getterType == null
                        || !KotlinTypeChecker.DEFAULT.isSubtypeOf(getterType, setterType)
                    ) {
                        if (isInterfaceClass()) return@mapNotNull null
                        false
                    } else true
                }
                if (realGetter == null && realSetter == null) return@mapNotNull null
                val realProperty = group.firstIsInstanceOrNull<RealProperty>()
                val name = realGetter?.name ?: realSetter?.name!!

                val superDeclarationOwner = (realGetter?.function ?: realSetter?.function)
                    ?.resolveToDescriptorIfAny(resolutionFacade)
                    ?.overriddenDescriptors
                    ?.firstOrNull()
                    ?.findPsi()
                    ?.safeAs<KtDeclaration>()
                    ?.containingClassOrObject

                collectingState.propertyNameToSuperType[superDeclarationOwner to name]
                val type = collectingState.propertyNameToSuperType[superDeclarationOwner to name]
                    ?: calculatePropertyType(
                        realGetter?.function,
                        realSetter?.function
                    ) ?: return@mapNotNull null
                collectingState.propertyNameToSuperType[this to name] = type

                PropertyData(
                    realProperty,
                    realGetter?.withTargetSet(realProperty),
                    realSetter?.withTargetSet(realProperty),
                    type
                )
            }
    }

    private fun List<PropertyData>.filterGettersAndSetters(
        klass: KtClassOrObject,
        factory: KtPsiFactory
    ): List<PropertyWithAccessors> {
        val classDescriptor = klass.resolveToDescriptorIfAny(resolutionFacade) ?: return emptyList()

        val variablesDescriptorsMap =
            (listOfNotNull(classDescriptor.getSuperClassNotAny()) + classDescriptor.getSuperInterfaces())
                .flatMap { superClass ->
                    superClass
                        .unsubstitutedMemberScope
                        .getDescriptorsFiltered(DescriptorKindFilter.VARIABLES)
                }
                .asSequence()
                .filterIsInstance<VariableDescriptor>()
                .associateBy { it.name.asString() }

        return mapNotNull { (realProperty, realGetter, realSetter, type) ->
            val name = realGetter?.name ?: realSetter!!.name
            val typeStringified = type
                .takeUnless { it.isError }
                ?.let {
                    IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
                } ?: realGetter?.function?.typeReference?.text
            ?: realSetter?.function?.valueParameters?.firstOrNull()?.typeReference?.text!!

            if (realSetter != null
                && realGetter != null
                && klass.isInterfaceClass()
                && realSetter.function.hasOverrides() != realGetter.function.hasOverrides()
            ) return@mapNotNull null

            if (realSetter != null
                && realGetter != null
                && realSetter.function.hasSuperFunction() != realGetter.function.hasSuperFunction()
            ) return@mapNotNull null

            if (realProperty != null
                && realGetter?.target != null
                && realGetter.target != realProperty.property
                && realProperty.property.hasInitializer()
            ) return@mapNotNull null

            if (realGetter?.function?.hasInvalidSuperDescriptors() == true
                || realSetter?.function?.hasInvalidSuperDescriptors() == true
            ) return@mapNotNull null

            if (realProperty == null) {
                if (causesNameConflictInCurrentDeclarationAndItsParents(
                        name,
                        classDescriptor.containingDeclaration
                    )
                ) return@mapNotNull null
            }

            if (realProperty != null && (realGetter != null && !realGetter.isPure || realSetter != null && !realSetter.isPure)) {
                if (!realProperty.property.isPrivate()) return@mapNotNull null
                val hasUsages =
                    realProperty.property.hasUsagesOutsideOf(
                        klass.containingKtFile,
                        listOfNotNull(realGetter?.function, realSetter?.function)
                    )
                if (hasUsages) return@mapNotNull null
            }

            if (realSetter != null && realProperty != null) {
                val assignFieldOfOtherInstance = realProperty.property.usages().any { usage ->
                    val element = usage.safeAs<KtSimpleNameReference>()?.element ?: return@any false
                    if (!element.readWriteAccess(useResolveForReadWrite = true).isWrite) return@any false
                    val parent = element.parent
                    parent is KtQualifiedExpression && !parent.receiverExpression.isReferenceToThis()
                }
                if (assignFieldOfOtherInstance) return@mapNotNull null
            }

            if (realGetter != null && realProperty != null) {
                val getFieldOfOtherInstanceInGetter = realProperty.property.usages().any { usage ->
                    val element = usage.safeAs<KtSimpleNameReference>()?.element ?: return@any false
                    val parent = element.parent
                    parent is KtQualifiedExpression
                            && !parent.receiverExpression.isReferenceToThis()
                            && realGetter.function.isAncestor(element)
                }
                if (getFieldOfOtherInstanceInGetter) return@mapNotNull null
            }


            val getter = realGetter ?: when {
                realProperty?.property?.resolveToDescriptorIfAny(resolutionFacade)?.overriddenDescriptors?.any {
                    it.safeAs<VariableDescriptor>()?.isVar == true
                } == true -> FakeGetter(name, null, "")

                variablesDescriptorsMap[name]?.let { variable ->
                    variable.isVar && variable.containingDeclaration != classDescriptor
                } == true ->
                    FakeGetter(name, factory.createExpression("super.$name"), "")

                else -> return@mapNotNull null
            }

            val mergedProperty =
                if (getter is RealGetter
                    && getter.target != null
                    && getter.target!!.name != getter.name
                    && (realSetter == null || realSetter.target != null)
                ) {
                    MergedProperty(name, typeStringified, realSetter != null, getter.target!!)
                } else null

            val setter = realSetter ?: when {
                realProperty?.property?.isVar == true ->
                    FakeSetter(name, null, "")

                realProperty?.property?.resolveToDescriptorIfAny(resolutionFacade)?.overriddenDescriptors?.any {
                    it.safeAs<VariableDescriptor>()?.isVar == true
                } == true
                        || variablesDescriptorsMap[name]?.isVar == true ->
                    FakeSetter(
                        name,
                        factory.createBlock("super.$name = $name"),
                        ""
                    )

                realGetter != null
                        && (realProperty != null
                        && realProperty.property.visibilityModifierTypeOrDefault() != realGetter.function.visibilityModifierTypeOrDefault()
                        && realProperty.property.isVar
                        || mergedProperty != null
                        && mergedProperty.mergeTo.visibilityModifierTypeOrDefault() != realGetter.function.visibilityModifierTypeOrDefault()
                        && mergedProperty.mergeTo.isVar
                        ) ->
                    FakeSetter(name, null, null)

                else -> null
            }
            val isVar = setter != null
            val property = mergedProperty?.copy(isVar = isVar)
                ?: realProperty?.copy(isVar = isVar)
                ?: FakeProperty(name, typeStringified, isVar)

            PropertyWithAccessors(
                property,
                getter,
                setter
            )
        }
    }

    private fun KtProperty.renameTo(newName: String, factory: KtPsiFactory) {
        for (usage in usages()) {
            val element = usage.element
            val isBackingField = element is KtNameReferenceExpression
                    && element.text == KtTokens.FIELD_KEYWORD.value
                    && element.mainReference.resolve() == this
                    && isAncestor(element)
            if (isBackingField) continue
            val replacer =
                if (element.parent is KtQualifiedExpression) factory.createExpression(newName)
                else factory.createExpression("this.$newName")
            element.replace(replacer)
        }
        setName(newName)
    }

    private fun convertClass(
        klass: KtClassOrObject,
        propertiesData: List<PropertyData>
    ) {
        val factory = KtPsiFactory(klass)
        val accessors = propertiesData.filterGettersAndSetters(klass, factory)

        for ((property, getter, setter) in accessors) {
            val ktProperty = when (property) {
                is RealProperty -> {
                    if (property.property.isVar != property.isVar) {
                        property.property.valOrVarKeyword.replace(
                            if (property.isVar) factory.createVarKeyword() else factory.createValKeyword()
                        )
                    }
                    property.property
                }
                is FakeProperty -> factory.createProperty(property.name, property.type, property.isVar).let {
                    val anchor = getter.safeAs<RealAccessor>()?.function ?: setter.cast<RealAccessor>().function
                    klass.addDeclarationBefore(it, anchor)
                }
                is MergedProperty -> {
                    property.mergeTo
                }
            }


            val propertyInfo = when (property) {
                is RealProperty -> property.property.fqNameWithoutCompanions.let(externalCodeUpdater::getMember)
                is MergedProperty -> property.mergeTo.fqNameWithoutCompanions.let(externalCodeUpdater::getMember)
                is FakeProperty -> JKFakeFieldData(
                    isStatic = klass is KtObjectDeclaration,
                    kotlinElementPointer = null,
                    fqName = klass.fqNameWithoutCompanions.child(Name.identifier(property.name)),
                    name = property.name
                ).also { externalCodeUpdater.addMember(it) }
            }?.also { it.name = property.name } as? JKFieldData


            val getterFqName = getter.safeAs<RealGetter>()?.function?.fqNameWithoutCompanions
            val setterFqName = setter.safeAs<RealSetter>()?.function?.fqNameWithoutCompanions

            getterFqName?.let { fqName ->
                externalCodeUpdater.getMember(fqName)?.safeAs<JKMethodData>()?.let {
                    it.usedAsAccessorOfProperty = propertyInfo ?: return@let
                }
            }

            setterFqName?.let { fqName ->
                externalCodeUpdater.getMember(fqName)?.safeAs<JKMethodData>()?.let {
                    it.usedAsAccessorOfProperty = propertyInfo ?: return@let
                }
            }

            val isOpen = getter.safeAs<RealGetter>()?.function?.hasModifier(KtTokens.OPEN_KEYWORD) == true
                    || setter.safeAs<RealSetter>()?.function?.hasModifier(KtTokens.OPEN_KEYWORD) == true

            val ktGetter = addGetter(getter, ktProperty, factory, property.isFake)
            val ktSetter =
                setter?.let {
                    createSetter(it, ktProperty, factory, property.isFake)
                }
            val getterVisibility = getter.safeAs<RealGetter>()?.function?.visibilityModifierTypeOrDefault()
            if (getter is RealGetter) {
                if (getter.function.isAbstract()) {
                    ktProperty.addModifier(KtTokens.ABSTRACT_KEYWORD)
                }
                val commentSaver = CommentSaver(getter.function)
                getter.function.delete()
                commentSaver.restore(ktProperty)
            }
            if (setter is RealSetter) {
                val commentSaver = CommentSaver(setter.function)
                setter.function.delete()
                commentSaver.restore(ktProperty)
            }

            // If getter & setter do not have backing fields we should remove initializer
            // As we already know that property is not directly used in the code
            if (getter.target == null && setter?.target == null) {
                ktProperty.initializer = null
            }

            val propertyVisibility = ktProperty.visibilityModifierTypeOrDefault()
            getterVisibility?.let { ktProperty.setVisibility(it) }
            if (ktSetter != null) {
                if (setter !is RealSetter) {
                    ktSetter.setVisibility(propertyVisibility)
                }
                ktProperty.addAfter(ktSetter, ktGetter)
            }

            if (property is MergedProperty) {
                ktProperty.renameTo(property.name, factory)
            }
            if (isOpen) {
                ktProperty.addModifier(KtTokens.OPEN_KEYWORD)
            }
        }
    }
}


private data class PropertyWithAccessors(
    val property: Property,
    val getter: Getter,
    val setter: Setter?
)

private data class PropertyData(
    val realProperty: RealProperty?,
    val realGetter: RealGetter?,
    val realSetter: RealSetter?,
    val type: KotlinType
)

private interface PropertyInfo {
    val name: String
}

private interface Accessor : PropertyInfo {
    val target: KtProperty?
    val body: KtExpression?
    val modifiersText: String?
    val isPure: Boolean
}

private sealed class Property : PropertyInfo {
    abstract val isVar: Boolean
}

private data class RealProperty(
    val property: KtProperty,
    override val name: String,
    override val isVar: Boolean = property.isVar
) : Property()

private data class FakeProperty(override val name: String, val type: String, override val isVar: Boolean) : Property()
private data class MergedProperty(
    override val name: String,
    val type: String,
    override val isVar: Boolean,
    val mergeTo: KtProperty
) : Property()

private sealed class Getter : Accessor
private sealed class Setter : Accessor {
    abstract val parameterName: String
}

private interface FakeAccessor : Accessor {
    override val target: KtProperty?
        get() = null
    override val isPure: Boolean
        get() = true
}

private interface RealAccessor : Accessor {
    val function: KtNamedFunction
    override val body: KtExpression?
        get() = function.bodyExpression
    override val modifiersText: String
        get() = function.modifierList?.text.orEmpty()

    fun updateTarget(newTarget: KtProperty): RealAccessor
}

private data class RealGetter(
    override val function: KtNamedFunction,
    override val target: KtProperty?,
    override val name: String,
    override val isPure: Boolean
) : Getter(), RealAccessor {
    override fun updateTarget(newTarget: KtProperty): RealGetter =
        copy(target = newTarget)
}

private data class FakeGetter(
    override val name: String,
    override val body: KtExpression?,
    override val modifiersText: String
) : Getter(), FakeAccessor

private data class RealSetter(
    override val function: KtNamedFunction,
    override val target: KtProperty?,
    override val name: String,
    override val isPure: Boolean
) : Setter(), RealAccessor {
    override val parameterName: String
        get() = (function.valueParameters.first().name ?: name).fixSetterParameterName()

    override fun updateTarget(newTarget: KtProperty): RealSetter =
        copy(target = newTarget)
}

private data class FakeSetter(
    override val name: String,
    override val body: KtExpression?,
    override val modifiersText: String?
) : Setter(), FakeAccessor {
    override val parameterName: String
        get() = name.fixSetterParameterName()
}

private data class CollectingState(
    val propertyNameToSuperType: MutableMap<Pair<KtClassOrObject, String>, KotlinType> = mutableMapOf()
)

private fun String.fixSetterParameterName() =
    if (this == KtTokens.FIELD_KEYWORD.value) "value"
    else this

private val PropertyInfo.isFake: Boolean
    get() = this is FakeAccessor
