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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.MethodSignatureUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo.project
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.hasJavaResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.findAnalyzerServices
import org.jetbrains.kotlin.idea.references.unwrappedTargets
import org.jetbrains.kotlin.idea.search.ReceiverTypeSearcherInfo
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isTypeRefinementEnabled
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.isValidOperator

fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String? =
    declaration.descriptor?.let { DescriptorRenderer.COMPACT.render(it) }

fun isSamInterface(psiClass: PsiClass): Boolean {
    val classDescriptor = psiClass.getJavaMemberDescriptor() as? JavaClassDescriptor
    return classDescriptor != null && getSingleAbstractMethodOrNull(classDescriptor) != null
}

fun hasType(element: KtExpression): Boolean =
    element.analyze(BodyResolveMode.PARTIAL).getType(element) != null

val KtDeclaration.constructor: ConstructorDescriptor?
    get() {
        val context = this.analyze()
        return when (this) {
            is KtClassOrObject -> context[BindingContext.CLASS, this]?.unsubstitutedPrimaryConstructor
            is KtFunction -> context[BindingContext.CONSTRUCTOR, this]
            else -> null
        }
    }

val KtParameter.propertyDescriptor: PropertyDescriptor?
    get() = this.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? PropertyDescriptor

fun PsiReference.checkUsageVsOriginalDescriptor(
    targetDescriptor: DeclarationDescriptor,
    declarationToDescriptor: (KtDeclaration) -> DeclarationDescriptor? = { it.descriptor },
    checker: (usageDescriptor: DeclarationDescriptor, targetDescriptor: DeclarationDescriptor) -> Boolean
): Boolean {
    return unwrappedTargets
        .filterIsInstance<KtDeclaration>()
        .any {
            val usageDescriptor = declarationToDescriptor(it)
            usageDescriptor != null && checker(usageDescriptor, targetDescriptor)
        }
}

fun PsiReference.isConstructorUsage(ktClassOrObject: KtClassOrObject): Boolean = with(element) {
    fun checkJavaUsage(): Boolean {
        val call = getNonStrictParentOfType<PsiConstructorCall>()
        return call == parent && call?.resolveConstructor()?.containingClass?.navigationElement == ktClassOrObject
    }

    fun checkKotlinUsage(): Boolean {
        if (this !is KtElement) return false

        val descriptor = getConstructorCallDescriptor() as? ConstructorDescriptor ?: return false

        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.containingDeclaration)
        return declaration == ktClassOrObject || (declaration is KtConstructor<*> && declaration.getContainingClassOrObject() == ktClassOrObject)
    }

    checkJavaUsage() || checkKotlinUsage()
}

private fun KtElement.getConstructorCallDescriptor(): DeclarationDescriptor? {
    val bindingContext = this.analyze()
    val constructorCalleeExpression = getNonStrictParentOfType<KtConstructorCalleeExpression>()
    if (constructorCalleeExpression != null) {
        return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.constructorReferenceExpression)
    }

    val callExpression = getNonStrictParentOfType<KtCallElement>()
    if (callExpression != null) {
        val callee = callExpression.calleeExpression
        if (callee is KtReferenceExpression) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
        }
    }

    return null
}

fun PsiElement.processDelegationCallConstructorUsages(scope: SearchScope, process: (KtCallElement) -> Boolean): Boolean {
    val task = buildProcessDelegationCallConstructorUsagesTask(scope, process)
    return task()
}

// should be executed under read-action, returns long-running part to be executed outside read-action
fun PsiElement.buildProcessDelegationCallConstructorUsagesTask(scope: SearchScope, process: (KtCallElement) -> Boolean): () -> Boolean {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    val task1 = buildProcessDelegationCallKotlinConstructorUsagesTask(scope, process)
    val task2 = buildProcessDelegationCallJavaConstructorUsagesTask(scope, process)
    return { task1() && task2() }
}

private fun PsiElement.buildProcessDelegationCallKotlinConstructorUsagesTask(
    scope: SearchScope,
    process: (KtCallElement) -> Boolean
): () -> Boolean {
    val element = unwrapped
    if (element != null && element !in scope) return { true }

    val klass = when (element) {
        is KtConstructor<*> -> element.getContainingClassOrObject()
        is KtClass -> element
        else -> return { true }
    }

    if (klass !is KtClass || element !is KtDeclaration) return { true }
    val descriptor = lazyPub { element.constructor }

    if (!processClassDelegationCallsToSpecifiedConstructor(klass, descriptor, process)) return { false }

    // long-running task, return it to execute outside read-action
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process) }
}

private fun PsiElement.buildProcessDelegationCallJavaConstructorUsagesTask(
    scope: SearchScope,
    process: (KtCallElement) -> Boolean
): () -> Boolean {
    if (this is KtLightElement<*, *>) return { true }
    // TODO: Temporary hack to avoid NPE while KotlinNoOriginLightMethod is around
    if (this is KtLightMethod && this.kotlinOrigin == null) return { true }
    if (!(this is PsiMethod && isConstructor)) return { true }
    val klass = containingClass ?: return { true }
    val descriptor = lazyPub { getJavaMethodDescriptor() as? ConstructorDescriptor }
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, descriptor, process) }
}


private fun processInheritorsDelegatingCallToSpecifiedConstructor(
    klass: PsiElement,
    scope: SearchScope,
    lazyDescriptor: Lazy<ConstructorDescriptor?>,
    process: (KtCallElement) -> Boolean
): Boolean {
    return HierarchySearchRequest(klass, scope, false).searchInheritors().all {
        runReadAction {
            val unwrapped = it.takeIf { it.isValid }?.unwrapped
            if (unwrapped is KtClass)
                processClassDelegationCallsToSpecifiedConstructor(unwrapped, lazyDescriptor, process)
            else
                true
        }
    }
}

private fun processClassDelegationCallsToSpecifiedConstructor(
    klass: KtClass,
    lazyDescriptor: Lazy<ConstructorDescriptor?>,
    process: (KtCallElement) -> Boolean
): Boolean {
    for (secondaryConstructor in klass.secondaryConstructors) {
        val delegationCallDescriptor =
            secondaryConstructor.getDelegationCall().getConstructorCallDescriptor()
                ?: continue

        if (lazyDescriptor.value == delegationCallDescriptor) {
            if (!process(secondaryConstructor.getDelegationCall())) return false
        }
    }
    if (!klass.isEnum()) return true
    for (declaration in klass.declarations) {
        if (declaration is KtEnumEntry) {
            val delegationCall =
                declaration.superTypeListEntries.firstOrNull() as? KtSuperTypeCallEntry
                    ?: continue
            val constructorCallDescriptor =
                delegationCall.calleeExpression.getConstructorCallDescriptor()
                    ?: continue

            if (lazyDescriptor.value == constructorCallDescriptor) {
                if (!process(delegationCall)) return false
            }
        }
    }
    return true
}

// Check if reference resolves to extension function whose receiver is the same as declaration's parent (or its superclass)
// Used in extension search
fun PsiReference.isExtensionOfDeclarationClassUsage(declaration: KtNamedDeclaration): Boolean {
    val descriptor = declaration.descriptor ?: return false
    return checkUsageVsOriginalDescriptor(descriptor) { usageDescriptor, targetDescriptor ->
        when (usageDescriptor) {
            targetDescriptor -> false
            !is FunctionDescriptor -> false
            else -> {
                val receiverDescriptor =
                    usageDescriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor
                val containingDescriptor = targetDescriptor.containingDeclaration

                containingDescriptor == receiverDescriptor
                        || (containingDescriptor is ClassDescriptor
                        && receiverDescriptor is ClassDescriptor
                        && DescriptorUtils.isSubclass(containingDescriptor, receiverDescriptor))
            }
        }
    }
}

// Check if reference resolves to the declaration with the same parent
// Used in overload search
fun PsiReference.isUsageInContainingDeclaration(declaration: KtNamedDeclaration): Boolean {
    val descriptor = declaration.descriptor ?: return false
    return checkUsageVsOriginalDescriptor(descriptor) { usageDescriptor, targetDescriptor ->
        usageDescriptor != targetDescriptor
                && usageDescriptor.containingDeclaration == targetDescriptor.containingDeclaration
    }
}

fun PsiReference.isCallableOverrideUsage(declaration: KtNamedDeclaration): Boolean {
    val toDescriptor: (KtDeclaration) -> CallableDescriptor? = { sourceDeclaration ->
        if (sourceDeclaration is KtParameter) {
            // we don't treat parameters in overriding method as "override" here (overriding parameters usages are searched optionally and via searching of overriding methods first)
            if (sourceDeclaration.hasValOrVar()) sourceDeclaration.propertyDescriptor else null
        } else {
            sourceDeclaration.descriptor as? CallableDescriptor
        }
    }

    val targetDescriptor = toDescriptor(declaration) ?: return false

    return unwrappedTargets.any {
        when (it) {
            is KtDeclaration -> {
                val usageDescriptor = toDescriptor(it)
                usageDescriptor != null && OverridingUtil.overrides(
                    usageDescriptor,
                    targetDescriptor,
                    usageDescriptor.module.isTypeRefinementEnabled(),
                    false // don't distinguish between expect and non-expect callable descriptors, KT-38298, KT-38589
                )
            }
            is PsiMethod -> {
                declaration.toLightMethods().any { superMethod -> MethodSignatureUtil.isSuperMethod(superMethod, it) }
            }
            else -> false
        }
    }
}

fun PsiElement.searchReferencesOrMethodReferences(): Collection<PsiReference> {
    val lightMethods = toLightMethods()
    return if (lightMethods.isNotEmpty()) {
        lightMethods.flatMapTo(LinkedHashSet()) { MethodReferencesSearch.search(it) }
    } else {
        ReferencesSearch.search(this).findAll()
    }
}

fun <T : PsiNamedElement> List<T>.filterDataClassComponentsIfDisabled(kotlinOptions: KotlinReferencesSearchOptions): List<T> {
    if (kotlinOptions.searchForComponentConventions) return this

    fun PsiNamedElement.isComponentElement(): Boolean {

        if (this !is PsiMethod) return false

        val dataClassParent = ((parent as? KtLightClass)?.kotlinOrigin as? KtClass)?.isData() == true
        if (!dataClassParent) return false

        if (!Name.isValidIdentifier(name)) return false
        val nameIdentifier = Name.identifier(name)
        if (!DataClassDescriptorResolver.isComponentLike(nameIdentifier)) return false

        return true
    }

    return filter { !it.isComponentElement() }
}

fun KtFile.forceResolveReferences(elements: List<KtElement>) {
    getResolutionFacade().analyze(elements, BodyResolveMode.PARTIAL)
}

private fun PsiElement.resolveTargetToDescriptor(isDestructionDeclarationSearch: Boolean): FunctionDescriptor? {

    if (isDestructionDeclarationSearch && this is KtParameter) {
        return dataClassComponentFunction()
    }

    return when {
        this is KtDeclaration -> resolveToDescriptorIfAny(BodyResolveMode.FULL)
        this is PsiMember && hasJavaResolutionFacade() ->
            this.getJavaOrKotlinMemberDescriptor()
        else -> null
    } as? FunctionDescriptor
}

private fun containsTypeOrDerivedInside(declaration: KtDeclaration, typeToSearch: FuzzyType): Boolean {

    fun KotlinType.containsTypeOrDerivedInside(type: FuzzyType): Boolean {
        return type.checkIsSuperTypeOf(this) != null || arguments.any { !it.isStarProjection && it.type.containsTypeOrDerivedInside(type) }
    }

    val descriptor = declaration.resolveToDescriptorIfAny() as? CallableDescriptor
    val type = descriptor?.returnType
    return type != null && type.containsTypeOrDerivedInside(typeToSearch)
}

private fun FuzzyType.toPsiClass(project: Project): PsiClass? {
    val classDescriptor = type.constructor.declarationDescriptor ?: return null
    val classDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor)
    return when (classDeclaration) {
        is PsiClass -> classDeclaration
        is KtClassOrObject -> classDeclaration.toLightClass()
        else -> null
    }
}

private fun PsiElement.extractReceiverType(isDestructionDeclarationSearch: Boolean): FuzzyType? {
    val descriptor = resolveTargetToDescriptor(isDestructionDeclarationSearch)?.takeIf { it.isValidOperator() } ?: return null

    return if (descriptor.isExtension) {
        descriptor.fuzzyExtensionReceiverType()!!
    } else {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
        classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
    }
}

fun PsiElement.getReceiverTypeSearcherInfo(isDestructionDeclarationSearch: Boolean): ReceiverTypeSearcherInfo? {
    val receiverType = runReadAction { extractReceiverType(isDestructionDeclarationSearch) } ?: return null
    val psiClass = runReadAction { receiverType.toPsiClass(project) }
    return ReceiverTypeSearcherInfo(psiClass) {
        containsTypeOrDerivedInside(it, receiverType)
    }
}

fun KtFile.getDefaultImports(): List<ImportPath> {
    val moduleInfo = getNullableModuleInfo() ?: return emptyList()
    return TargetPlatformDetector.getPlatform(this).findAnalyzerServices(project).getDefaultImports(
        IDELanguageSettingsProvider.getLanguageVersionSettings(moduleInfo, project),
        includeLowPriorityImports = true
    )
}

fun PsiFile.scriptDefinitionExists(): Boolean = findScriptDefinition() != null