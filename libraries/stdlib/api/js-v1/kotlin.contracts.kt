@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun contract(builder: kotlin.contracts.ContractBuilder.() -> kotlin.Unit): kotlin.Unit

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface CallsInPlace : kotlin.contracts.Effect {
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface ConditionalEffect : kotlin.contracts.Effect {
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface ContractBuilder {
    @kotlin.internal.ContractsDsl
    public abstract fun <R> callsInPlace(lambda: kotlin.Function<R>, kind: kotlin.contracts.InvocationKind = ...): kotlin.contracts.CallsInPlace

    @kotlin.internal.ContractsDsl
    public abstract fun returns(): kotlin.contracts.Returns

    @kotlin.internal.ContractsDsl
    public abstract fun returns(value: kotlin.Any?): kotlin.contracts.Returns

    @kotlin.internal.ContractsDsl
    public abstract fun returnsNotNull(): kotlin.contracts.ReturnsNotNull
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface Effect {
}

@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.3")
@kotlin.RequiresOptIn
@kotlin.annotation.MustBeDocumented
public final annotation class ExperimentalContracts : kotlin.Annotation {
    public constructor ExperimentalContracts()
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public final enum class InvocationKind : kotlin.Enum<kotlin.contracts.InvocationKind> {
    @kotlin.internal.ContractsDsl
    enum entry AT_MOST_ONCE

    @kotlin.internal.ContractsDsl
    enum entry AT_LEAST_ONCE

    @kotlin.internal.ContractsDsl
    enum entry EXACTLY_ONCE

    @kotlin.internal.ContractsDsl
    enum entry UNKNOWN
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface Returns : kotlin.contracts.SimpleEffect {
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface ReturnsNotNull : kotlin.contracts.SimpleEffect {
}

@kotlin.internal.ContractsDsl
@kotlin.contracts.ExperimentalContracts
@kotlin.SinceKotlin(version = "1.3")
public interface SimpleEffect : kotlin.contracts.Effect {
    @kotlin.internal.ContractsDsl
    @kotlin.contracts.ExperimentalContracts
    public abstract infix fun implies(booleanExpression: kotlin.Boolean): kotlin.contracts.ConditionalEffect
}