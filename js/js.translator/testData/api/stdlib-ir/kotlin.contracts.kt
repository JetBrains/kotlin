package kotlin.contracts

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun contract(/*0*/ builder: kotlin.contracts.ContractBuilder.() -> kotlin.Unit): kotlin.Unit

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface CallsInPlace : kotlin.contracts.Effect {
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface ConditionalEffect : kotlin.contracts.Effect {
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface ContractBuilder {
    @kotlin.internal.ContractsDsl public abstract fun </*0*/ R> callsInPlace(/*0*/ lambda: kotlin.Function<R>, /*1*/ kind: kotlin.contracts.InvocationKind = ...): kotlin.contracts.CallsInPlace
    @kotlin.internal.ContractsDsl public abstract fun returns(): kotlin.contracts.Returns
    @kotlin.internal.ContractsDsl public abstract fun returns(/*0*/ value: kotlin.Any?): kotlin.contracts.Returns
    @kotlin.internal.ContractsDsl public abstract fun returnsNotNull(): kotlin.contracts.ReturnsNotNull
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface Effect {
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.3") @kotlin.Experimental @kotlin.RequiresOptIn @kotlin.annotation.MustBeDocumented public final annotation class ExperimentalContracts : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalContracts()
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public final enum class InvocationKind : kotlin.Enum<kotlin.contracts.InvocationKind> {
    @kotlin.internal.ContractsDsl enum entry AT_MOST_ONCE

    @kotlin.internal.ContractsDsl enum entry AT_LEAST_ONCE

    @kotlin.internal.ContractsDsl enum entry EXACTLY_ONCE

    @kotlin.internal.ContractsDsl enum entry UNKNOWN

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.contracts.InvocationKind
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.contracts.InvocationKind>
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface Returns : kotlin.contracts.SimpleEffect {
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface ReturnsNotNull : kotlin.contracts.SimpleEffect {
}

@kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts @kotlin.SinceKotlin(version = "1.3") public interface SimpleEffect : kotlin.contracts.Effect {
    @kotlin.internal.ContractsDsl @kotlin.contracts.ExperimentalContracts public abstract infix fun implies(/*0*/ booleanExpression: kotlin.Boolean): kotlin.contracts.ConditionalEffect
}