package kotlin_native

public open class Any0 {
    // @SymbolName("Kotlin_Any_equals")
    // external public open operator fun equals0(other: Any0?): Boolean

    // TODO: do we need that in Any?
    @SymbolName("Kotlin_Any_hashCode")
    external public open fun hashCode0(): Int

    @SymbolName("Kotlin_Any_toString")
    external public open fun toString0(): String
}
