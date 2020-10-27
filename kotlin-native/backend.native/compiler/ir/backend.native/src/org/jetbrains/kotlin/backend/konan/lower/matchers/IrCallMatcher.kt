package org.jetbrains.kotlin.backend.konan.lower.matchers

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin


internal interface IrCallMatcher : (IrCall) -> Boolean

/**
 * IrCallMatcher that puts restrictions only on its callee.
 */
internal class SimpleCalleeMatcher(
        restrictions: IrFunctionMatcherContainer.() -> Unit
) : IrCallMatcher {

    private val calleeRestriction: IrFunctionMatcher
            = createIrFunctionRestrictions(restrictions)

    override fun invoke(call: IrCall) = calleeRestriction(call.symbol.owner)
}

internal class IrCallExtensionReceiverMatcher(
        val restriction: (IrExpression?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.extensionReceiver)
}

internal class IrCallDispatchReceiverMatcher(
        val restriction: (IrExpression?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.dispatchReceiver)
}

internal class IrCallOriginMatcher(
        val restriction: (IrStatementOrigin?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.origin)
}

internal open class IrCallMatcherContainer : IrCallMatcher {

    private val matchers = mutableListOf<IrCallMatcher>()

    fun add(matcher: IrCallMatcher) {
        matchers += matcher
    }

    fun extensionReceiver(restriction: (IrExpression?) -> Boolean) =
            add(IrCallExtensionReceiverMatcher(restriction))

    fun origin(restriction: (IrStatementOrigin?) -> Boolean) =
            add(IrCallOriginMatcher(restriction))

    fun callee(restrictions: IrFunctionMatcherContainer.() -> Unit) {
        add(SimpleCalleeMatcher(restrictions))
    }

    fun dispatchReceiver(restriction: (IrExpression?) -> Boolean) =
            add(IrCallDispatchReceiverMatcher(restriction))

    override fun invoke(call: IrCall) = matchers.all { it(call) }
}

internal fun createIrCallMatcher(restrictions: IrCallMatcherContainer.() -> Unit) =
        IrCallMatcherContainer().apply(restrictions)