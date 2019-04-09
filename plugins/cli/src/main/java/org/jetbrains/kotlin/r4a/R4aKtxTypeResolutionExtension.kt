package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade

class R4aKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        val ktxCallResolver =
            KtxCallResolver(
                callResolver,
                facade,
                element.project,
                ComposableAnnotationChecker.get(element.project)
            )

        ktxCallResolver.initializeFromKtxElement(element, context)

        val temporaryForKtxCall =
            TemporaryTraceAndCache.create(context, "trace to resolve ktx call", element)

        val resolvedKtxElementCall = ktxCallResolver.resolveFromKtxElement(
            element,
            context.replaceTraceAndCache(temporaryForKtxCall)
        )

        temporaryForKtxCall.commit()

        context.trace.record(R4AWritableSlices.RESOLVED_KTX_CALL, element, resolvedKtxElementCall)
    }
}