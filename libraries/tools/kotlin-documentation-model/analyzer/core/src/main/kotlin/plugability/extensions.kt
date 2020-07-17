package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

data class ExtensionPoint<T : Any> internal constructor(
    internal val pluginClass: String,
    internal val pointName: String
) {
    override fun toString() = "ExtensionPoint: $pluginClass/$pointName"
}

sealed class OrderingKind {
    object None : OrderingKind()
    class ByDsl(val block: (OrderDsl.() -> Unit)) : OrderingKind()
}

sealed class OverrideKind {
    object None : OverrideKind()
    class Present(val overriden: Extension<*, *, *>) : OverrideKind()
}

class Extension<T : Any, Ordering : OrderingKind, Override : OverrideKind> internal constructor(
    internal val extensionPoint: ExtensionPoint<T>,
    internal val pluginClass: String,
    internal val extensionName: String,
    internal val action: LazyEvaluated<T>,
    internal val ordering: Ordering,
    internal val override: Override,
    internal val conditions: List<DokkaConfiguration.() -> Boolean>
) {
    override fun toString() = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?) =
        if (other is Extension<*, *, *>) this.pluginClass == other.pluginClass && this.extensionName == other.extensionName
        else false

    override fun hashCode() = listOf(pluginClass, extensionName).hashCode()

    val condition: DokkaConfiguration.() -> Boolean
        get() = { conditions.all { it(this) } }
}

private fun <T : Any> Extension(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>
) = Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.None, OverrideKind.None, emptyList())

@DslMarker
annotation class ExtensionsDsl

@ExtensionsDsl
class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {

    infix fun <T : Any> ExtensionPoint<T>.with(action: T) =
        Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromInstance(action))

    infix fun <T : Any> ExtensionPoint<T>.providing(action: (DokkaContext) -> T) =
        Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromRecipe(action))

    infix fun <T : Any, Override : OverrideKind> Extension<T, OrderingKind.None, Override>.order(
        block: OrderDsl.() -> Unit
    ) = Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.ByDsl(block), override, conditions)

    infix fun <T : Any, Override : OverrideKind, Ordering: OrderingKind> Extension<T, Ordering, Override>.applyIf(
        condition: DokkaConfiguration.() -> Boolean
    ) = Extension(extensionPoint, pluginClass, extensionName, action, ordering, override, conditions + condition)

    infix fun <T : Any, Override : OverrideKind, Ordering: OrderingKind> Extension<T, Ordering, Override>.override(
        overriden: Extension<T, *, *>
    ) = Extension(extensionPoint, pluginClass, extensionName, action, ordering, OverrideKind.Present(overriden), conditions)
}

@ExtensionsDsl
class OrderDsl {
    internal val previous = mutableSetOf<Extension<*, *, *>>()
    internal val following = mutableSetOf<Extension<*, *, *>>()

    fun after(vararg extensions: Extension<*, *, *>) {
        previous += extensions
    }

    fun before(vararg extensions: Extension<*, *, *>) {
        following += extensions
    }
}