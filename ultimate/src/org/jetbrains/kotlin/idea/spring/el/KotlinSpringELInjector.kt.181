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

package org.jetbrains.kotlin.idea.spring.el

import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.jsp.el.ELContextProvider
import com.intellij.spring.constants.SpringAnnotationsConstants
import com.intellij.spring.el.SpringELLanguage
import com.intellij.spring.el.SpringElTemplateParser
import com.intellij.spring.el.contextProviders.SpringElContextProvider
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlainWithEscapes
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinSpringELInjector : MultiHostInjector {
    companion object {
        private val RELEVANT_ANNOTATIONS = setOf(SpringAnnotationsConstants.VALUE, SpringAnnotationsConstants.SCHEDULED)

        // SpEL support requires both Spring and J2EE plugins
        // This code works around the inability to specify multiple dependencies for a given plugin config file
        private val contextRecorder by lazy {
            if ("com.intellij.javaee" in PluginManager.getDisabledPlugins()) return@lazy null
            { host: PsiElement -> host.putUserData(ELContextProvider.ourContextProviderKey, SpringElContextProvider(host)) }
        }
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        if (host is KtStringTemplateExpression) {
            if (!host.isPlainWithEscapes()) return

            val argument = host.parent as? KtValueArgument ?: return
            val entry = argument.getStrictParentOfType<KtAnnotationEntry>() ?: return
            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = entry.getResolvedCall(context) ?: return
            val annotation = (resolvedCall.resultingDescriptor as? ConstructorDescriptor)?.containingDeclaration ?: return
            if (annotation.importableFqName?.asString() !in RELEVANT_ANNOTATIONS) return

            val text = host.getText()
            val ranges = SpringElTemplateParser.parse(text).ifEmpty { return }
            for (range in ranges) {
                registrar
                        .startInjecting(SpringELLanguage.INSTANCE)
                        .addPlace(null, null, host as PsiLanguageInjectionHost, range)
                        .doneInjecting()
            }

            contextRecorder!!(host)
        }
    }

    override fun elementsToInjectIn() = contextRecorder?.let { listOf(KtStringTemplateExpression::class.java) } ?: emptyList()
}

