package org.jetbrains.kotlin.backend.konan.lower.matchers

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

internal interface IrFunctionMatcher : (IrFunction) -> Boolean

internal class ParameterMatcher(
        val index: Int,
        val restriction: (IrValueParameter) -> Boolean
) : IrFunctionMatcher {

    override fun invoke(function: IrFunction): Boolean {
        val params = function.valueParameters
        return params.size > index && restriction(params[index])
    }
}

internal class DispatchReceiverMatcher(
        val restriction: (IrValueParameter?) -> Boolean
) : IrFunctionMatcher {

    override fun invoke(function: IrFunction): Boolean {
        return restriction(function.dispatchReceiverParameter)
    }
}

internal class ExtensionReceiverMatcher(
        val restriction: (IrValueParameter?) -> Boolean
) : IrFunctionMatcher {

    override fun invoke(function: IrFunction): Boolean {
        return restriction(function.extensionReceiverParameter)
    }
}

internal class ParameterCountMatcher(
        val restriction: (Int) -> Boolean
) : IrFunctionMatcher {

    override fun invoke(function: IrFunction): Boolean {
        return restriction(function.valueParameters.size)
    }
}

internal class FqNameMatcher(
        val restriction: (FqName) -> Boolean
) : IrFunctionMatcher {

    override fun invoke(function: IrFunction): Boolean {
        return restriction(function.fqNameForIrSerialization)
    }
}

internal open class IrFunctionMatcherContainer : IrFunctionMatcher {
    private val restrictions = mutableListOf<IrFunctionMatcher>()

    fun add(restriction: IrFunctionMatcher) {
        restrictions += restriction
    }

    fun fqName(restriction: (FqName) -> Boolean) =
            add(FqNameMatcher(restriction))

    fun parameterCount(restriction: (Int) -> Boolean) =
            add(ParameterCountMatcher(restriction))

    fun extensionReceiver(restriction: (IrValueParameter?) -> Boolean) =
            add(ExtensionReceiverMatcher(restriction))

    fun dispatchReceiver(restriction: (IrValueParameter?) -> Boolean) =
            add(DispatchReceiverMatcher(restriction))

    fun parameter(index: Int, restriction: (IrValueParameter) -> Boolean) =
            add(ParameterMatcher(index, restriction))

    override fun invoke(function: IrFunction) = restrictions.all { it(function) }
}

internal fun createIrFunctionRestrictions(restrictions: IrFunctionMatcherContainer.() -> Unit) =
        IrFunctionMatcherContainer().apply(restrictions)

internal fun IrFunctionMatcherContainer.singleArgumentExtension(
        fqName: FqName,
        classes: Collection<IrClassSymbol>
): IrFunctionMatcherContainer {
    extensionReceiver { it != null && it.type.classifierOrNull in classes }
    parameterCount { it == 1 }
    fqName { it == fqName }
    return this
}

