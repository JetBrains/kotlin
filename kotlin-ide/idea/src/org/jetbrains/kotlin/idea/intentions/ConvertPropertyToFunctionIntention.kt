/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import java.util.*

class ConvertPropertyToFunctionIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java,
    KotlinBundle.lazyMessage("convert.property.to.function")
), LowPriorityAction {
    private inner class Converter(
        project: Project,
        descriptor: CallableDescriptor
    ) : CallableRefactoring<CallableDescriptor>(project, descriptor, text) {
        private val newName: String = JvmAbi.getterName(callableDescriptor.name.asString())

        private fun convertProperty(originalProperty: KtProperty, psiFactory: KtPsiFactory) {
            val property = originalProperty.copy() as KtProperty
            val getter = property.getter

            val sampleFunction = psiFactory.createFunction("fun foo() {\n\n}")

            property.valOrVarKeyword.replace(sampleFunction.funKeyword!!)
            property.addAfter(psiFactory.createParameterList("()"), property.nameIdentifier)
            if (property.initializer == null) {
                if (getter != null) {
                    val dropGetterTo = (getter.equalsToken ?: getter.bodyExpression)
                        ?.siblings(forward = false, withItself = false)
                        ?.firstOrNull { it !is PsiWhiteSpace }
                    getter.deleteChildRange(getter.firstChild, dropGetterTo)

                    val dropPropertyFrom = getter
                        .siblings(forward = false, withItself = false)
                        .first { it !is PsiWhiteSpace }
                        .nextSibling
                    property.deleteChildRange(dropPropertyFrom, getter.prevSibling)

                    val typeReference = property.typeReference
                    if (typeReference != null) {
                        property.addAfter(psiFactory.createWhiteSpace(), typeReference)
                    }
                }
            }
            property.setName(newName)
            property.annotationEntries.forEach {
                if (it.useSiteTarget != null) {
                    it.replace(psiFactory.createAnnotationEntry("@${it.shortName}${it.valueArgumentList?.text ?: ""}"))
                }
            }

            originalProperty.replace(psiFactory.createFunction(property.text))
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val propertyName = callableDescriptor.name.asString()
            val nameChanged = propertyName != newName
            val getterName = JvmAbi.getterName(callableDescriptor.name.asString())
            val conflicts = MultiMap<PsiElement, String>()
            val callables = getAffectedCallables(project, descriptorsForChange)
            val kotlinRefsToReplaceWithCall = ArrayList<KtSimpleNameExpression>()
            val refsToRename = ArrayList<PsiReference>()
            val javaRefsToReplaceWithCall = ArrayList<PsiReferenceExpression>()

            project.runSynchronouslyWithProgress(KotlinBundle.message("looking.for.usages.and.conflicts"), true) {
                runReadAction {
                    val progressIndicator = ProgressManager.getInstance().progressIndicator
                    progressIndicator.isIndeterminate = false
                    val progressStep = 1.0 / callables.size
                    for ((i, callable) in callables.withIndex()) {
                        progressIndicator.fraction = (i + 1) * progressStep

                        if (callable !is PsiNamedElement) continue

                        if (!checkModifiable(callable)) {
                            val renderedCallable = RefactoringUIUtil.getDescription(callable, true).capitalize()
                            conflicts.putValue(callable, KotlinBundle.message("can.t.modify.0", renderedCallable))
                        }

                        if (callable is KtParameter) {
                            conflicts.putValue(
                                callable,
                                if (callable.hasActualModifier()) KotlinBundle.message("property.has.an.actual.declaration.in.the.class.constructor")
                                else KotlinBundle.message("property.overloaded.in.child.class.constructor")
                            )
                        }

                        if (callable is KtProperty) {
                            callableDescriptor.getContainingScope()
                                ?.findFunction(callableDescriptor.name, NoLookupLocation.FROM_IDE) { it.valueParameters.isEmpty() }
                                ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                                ?.let { reportDeclarationConflict(conflicts, it) { s -> KotlinBundle.message("0.already.exists", s) } }
                        } else if (callable is PsiMethod) callable.checkDeclarationConflict(propertyName, conflicts, callables)

                        val usages = ReferencesSearch.search(callable)
                        for (usage in usages) {
                            if (usage is KtReference) {
                                if (usage is KtSimpleNameReference) {
                                    val expression = usage.expression
                                    if (expression.getCall(expression.analyze(BodyResolveMode.PARTIAL)) != null
                                        && expression.getStrictParentOfType<KtCallableReferenceExpression>() == null
                                    ) {
                                        kotlinRefsToReplaceWithCall.add(expression)
                                    } else if (nameChanged) {
                                        refsToRename.add(usage)
                                    }
                                } else {
                                    val refElement = usage.element
                                    conflicts.putValue(
                                        refElement,
                                        KotlinBundle.message(
                                            "unrecognized.reference.will.be.skipped.0",
                                            StringUtil.htmlEmphasize(refElement.text)
                                        )
                                    )
                                }
                                continue
                            }

                            val refElement = usage.element

                            if (refElement.text.endsWith(getterName)) continue

                            if (usage is PsiJavaReference) {
                                if (usage.resolve() is PsiField && usage is PsiReferenceExpression) {
                                    javaRefsToReplaceWithCall.add(usage)
                                }
                                continue
                            }

                            conflicts.putValue(
                                refElement,
                                KotlinBundle.message(
                                    "can.t.replace.foreign.reference.with.call.expression.0",
                                    StringUtil.htmlEmphasize(refElement.text)
                                )
                            )
                        }
                    }
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(text) {
                    val kotlinPsiFactory = KtPsiFactory(project)
                    val javaPsiFactory = PsiElementFactory.getInstance(project)
                    val newKotlinCallExpr = kotlinPsiFactory.createExpression("$newName()")

                    kotlinRefsToReplaceWithCall.forEach { it.replace(newKotlinCallExpr) }
                    refsToRename.forEach { it.handleElementRename(newName) }
                    javaRefsToReplaceWithCall.forEach {
                        val getterRef = it.handleElementRename(newName)
                        getterRef.replace(javaPsiFactory.createExpressionFromText("${getterRef.text}()", null))
                    }
                    callables.forEach {
                        when (it) {
                            is KtProperty -> convertProperty(it, kotlinPsiFactory)
                            is PsiMethod -> it.name = newName
                        }
                    }
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        val identifier = element.nameIdentifier ?: return false
        if (!identifier.textRange.containsOffset(caretOffset)) return false
        return element.delegate == null
                && !element.isVar
                && !element.isLocal
                && (element.initializer == null || element.getter == null)
                && !element.hasJvmFieldAnnotation()
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableDescriptor ?: return
        Converter(element.project, descriptor).run()
    }
}
