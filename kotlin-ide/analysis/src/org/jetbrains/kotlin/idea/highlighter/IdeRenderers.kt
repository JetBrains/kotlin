/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.highlighter.renderersUtil.renderResolvedCall
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData

object IdeRenderers {

    @JvmField
    val HTML_AMBIGUOUS_CALLS = Renderer { calls: Collection<ResolvedCall<*>> ->
        renderAmbiguousDescriptors(calls.map { it.resultingDescriptor })
    }

    @JvmField
    val HTML_COMPATIBILITY_CANDIDATE = Renderer { call: CallableDescriptor ->
        renderAmbiguousDescriptors(listOf(call))
    }

    @JvmField
    val HTML_AMBIGUOUS_REFERENCES = Renderer { descriptors: Collection<CallableDescriptor> ->
        renderAmbiguousDescriptors(descriptors)
    }

    private fun renderAmbiguousDescriptors(descriptors: Collection<CallableDescriptor>): String {
        val sortedDescriptors = descriptors.sortedWith(MemberComparator.INSTANCE)
        val context = RenderingContext.Impl(sortedDescriptors)
        return sortedDescriptors.joinToString("") { "<li>${HTML.render(it, context)}</li>" }
    }

    @JvmField
    val HTML_RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.HTML.withOptions {
        parameterNamesInFunctionalTypes = false
        modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
    })

    @JvmField
    val HTML_NONE_APPLICABLE_CALLS = Renderer { calls: Collection<ResolvedCall<*>> ->
        val context = RenderingContext.Impl(calls.map { it.resultingDescriptor })
        val comparator = compareBy(MemberComparator.INSTANCE) { c: ResolvedCall<*> -> c.resultingDescriptor }
        calls
            .sortedWith(comparator)
            .joinToString("") { "<li>${renderResolvedCall(it, context)}</li>" }
    }

    @JvmField
    val HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderConflictingSubstitutionsInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField
    val HTML_TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderParameterConstraintError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField
    val HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderNoInformationForParameterError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField
    val HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER = Renderer<InferenceErrorData> {
        Renderers.renderUpperBoundViolatedInferenceError(it, HtmlTabledDescriptorRenderer.create()).toString()
    }

    @JvmField
    val HTML_RENDER_RETURN_TYPE = ContextDependentRenderer<CallableMemberDescriptor> { member, context ->
        HTML_RENDER_TYPE.render(member.returnType!!, context)
    }

    @JvmField
    val HTML_CONFLICTING_JVM_DECLARATIONS_DATA = Renderer { data: ConflictingJvmDeclarationsData ->

        val descriptors = data.signatureOrigins
            .mapNotNull { it.descriptor }
            .sortedWith(MemberComparator.INSTANCE)
        val context = RenderingContext.of(descriptors)
        val conflicts = descriptors.joinToString("") { "<li>" + HTML.render(it, context) + "</li>\n" }

        KotlinIdeaAnalysisBundle.message(
            "the.following.declarations.have.the.same.jvm.signature.code.0.1.code.br.ul.2.ul",
            data.signature.name,
            data.signature.desc,
            conflicts
        )
    }

    @JvmField
    val HTML_THROWABLE = Renderer<Throwable> { throwable ->
        Renderers.THROWABLE.render(throwable).replace("\n", "<br/>")
    }

    @JvmField
    val HTML = DescriptorRenderer.HTML.withOptions {
        modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
    }.asRenderer()

    @JvmField
    val HTML_WITH_ANNOTATIONS = DescriptorRenderer.HTML.withOptions {
        modifiers = DescriptorRendererModifier.ALL
    }.asRenderer()

    @JvmField
    val HTML_WITH_ANNOTATIONS_WHITELIST = DescriptorRenderer.HTML.withAnnotationsWhitelist()
}
