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

package org.jetbrains.kotlin.idea.spring.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.spring.constants.SpringAnnotationsConstants
import com.intellij.spring.constants.SpringConstants
import com.intellij.spring.model.utils.resources.SpringResourcesBuilder
import com.intellij.spring.model.utils.resources.SpringResourcesUtil
import com.intellij.spring.references.SpringBeanNamesReferenceProvider
import com.intellij.spring.references.SpringBeanReference
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.AbstractKotlinReferenceContributor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi

// TODO: Use Kotlin patterns
class KotlinSpringReferenceContributor : AbstractKotlinReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerProvider<KtStringTemplateExpression> {
            if (!it.isPlain()) return@registerProvider null

            val callExpression = (it.parent as? KtValueArgument)?.getStrictParentOfType<KtCallExpression>() ?: return@registerProvider null
            val context = callExpression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = callExpression.getResolvedCall(context) ?: return@registerProvider null
            val callable = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return@registerProvider null
            if (!callable.overriddenTreeAsSequence(true).any {
                it.containingDeclaration.importableFqName?.asString() == SpringConstants.BEAN_FACTORY_CLASS
            }) return@registerProvider null
            if (callable.name.asString() !in SpringBeanNamesReferenceProvider.METHODS) return@registerProvider null

            SpringBeanReference(it, it.getContentRange())
        }

        registrar.registerProvider<KtStringTemplateExpression>(PsiReferenceRegistrar.HIGHER_PRIORITY) {
            if (!it.isPlain()) return@registerProvider null

            val argument = it.parent as? KtValueArgument ?: return@registerProvider null
            val argumentName = argument.getArgumentName() ?: return@registerProvider null
            if (argumentName.asName.asString() != "name") return@registerProvider null

            val entry = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return@registerProvider null
            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = entry.getResolvedCall(context) ?: return@registerProvider null
            val annotation = (resolvedCall.resultingDescriptor as? ConstructorDescriptor)?.containingDeclaration ?: return@registerProvider null
            if (annotation.importableFqName?.asString() != SpringAnnotationsConstants.JAVAX_RESOURCE) return@registerProvider null

            val contentRange = it.getContentRange()
            var startOffset = contentRange.startOffset
            val isFactoryBeanRef: Boolean
            if (it.plainContent.startsWith("&")) {
                startOffset++
                isFactoryBeanRef = true
            }
            else {
                isFactoryBeanRef = false
            }

            val callable = (it.getStrictParentOfType<KtAnnotationEntry>()?.parent as? KtModifierList)?.parent as? KtCallableDeclaration
            val callableType = (callable?.resolveToDescriptor() as? CallableDescriptor)?.returnType
            val requiredSuperClass = callableType?.constructor?.declarationDescriptor?.source?.getPsi() as? KtClass

            SpringBeanReference(it, TextRange(startOffset, contentRange.endOffset), requiredSuperClass?.toLightClass(), isFactoryBeanRef)
        }

        registrar.registerProvider<KtStringTemplateExpression>(PsiReferenceRegistrar.HIGHER_PRIORITY) {
            if (!it.isPlain()) return@registerProvider null

            val argument = it.parent as? KtValueArgument ?: return@registerProvider null
            val argumentName = argument.getArgumentName()
            if (argumentName != null && argumentName.asName.asString() != "value") return@registerProvider null

            val entry = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return@registerProvider null
            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = entry.getResolvedCall(context) ?: return@registerProvider null
            val annotation = (resolvedCall.resultingDescriptor as? ConstructorDescriptor)?.containingDeclaration ?: return@registerProvider null
            if (annotation.importableFqName?.asString() != SpringAnnotationsConstants.SCOPE) return@registerProvider null

            KtSpringBeanScopeReference(it)
        }

        registrar.registerMultiProvider<KtStringTemplateExpression> {
            if (!it.isPlain()) return@registerMultiProvider PsiReference.EMPTY_ARRAY

            val callExpression = (it.parent as? KtValueArgument)?.getStrictParentOfType<KtCallExpression>()
                                 ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val context = callExpression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = callExpression.getResolvedCall(context) ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val classDescriptor = (resolvedCall.resultingDescriptor as? ConstructorDescriptor)?.containingDeclaration
                                  ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val qName = classDescriptor.importableFqName?.asString()
            if (qName != SpringConstants.CLASS_PATH_XML_APP_CONTEXT && qName != SpringConstants.CLASS_PATH_RESOURCE) {
                return@registerMultiProvider PsiReference.EMPTY_ARRAY
            }

            val content = it.plainContent
            val resourcesBuilder = SpringResourcesBuilder.create(it, content).fromRoot(content.startsWith("/")).soft(false)
            SpringResourcesUtil.getInstance().getClassPathReferences(resourcesBuilder)
        }

        registrar.registerProvider<KtStringTemplateExpression>(PsiReferenceRegistrar.HIGHER_PRIORITY) {
            if (!it.isPlain()) return@registerProvider null

            val argument = it.parent as? KtValueArgument ?: return@registerProvider null
            val argumentName = argument.getArgumentName()
            if (argumentName != null && argumentName.asName.asString() != "value") return@registerProvider null

            val entry = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return@registerProvider null
            val bindingContext = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = entry.getResolvedCall(bindingContext) ?: return@registerProvider null
            val annotation = (resolvedCall.resultingDescriptor as? ConstructorDescriptor)?.containingDeclaration
                             ?: return@registerProvider null
            if (annotation.importableFqName?.asString() != SpringAnnotationsConstants.QUALIFIER) return@registerProvider null

            val annotated = entry.getStrictParentOfType<KtModifierListOwner>() ?: return@registerProvider null
            if (annotated is KtClassOrObject) {
                object : PsiReferenceBase<KtStringTemplateExpression>(it) {
                    override fun resolve() = entry
                    override fun getVariants(): Array<Any> = arrayOf()
                }
            }
            else {
                KtSpringQualifierReference(it)
            }
        }
    }
}