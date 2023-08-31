/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

public data class ExtensionPoint<T : Any> internal constructor(
    internal val pluginClass: String,
    internal val pointName: String
) {
    override fun toString(): String = "ExtensionPoint: $pluginClass/$pointName"
}

public sealed class OrderingKind {
    public object None : OrderingKind()

    public class ByDsl(
        public val block: (OrderDsl.() -> Unit)
    ) : OrderingKind()
}

public sealed class OverrideKind {
    public object None : OverrideKind()
    public class Present(
        public val overriden: List<Extension<*, *, *>>
    ) : OverrideKind()
}

public class Extension<T : Any, Ordering : OrderingKind, Override : OverrideKind> internal constructor(
    internal val extensionPoint: ExtensionPoint<T>,
    internal val pluginClass: String,
    internal val extensionName: String,
    internal val action: LazyEvaluated<T>,
    internal val ordering: Ordering,
    internal val override: Override,
    internal val conditions: List<DokkaConfiguration.() -> Boolean>
) {
    override fun toString(): String = "Extension: $pluginClass/$extensionName"

    override fun equals(other: Any?): Boolean =
        if (other is Extension<*, *, *>) this.pluginClass == other.pluginClass && this.extensionName == other.extensionName
        else false

    override fun hashCode(): Int = listOf(pluginClass, extensionName).hashCode()

    public val condition: DokkaConfiguration.() -> Boolean
        get() = { conditions.all { it(this) } }
}

internal fun <T : Any> Extension(
    extensionPoint: ExtensionPoint<T>,
    pluginClass: String,
    extensionName: String,
    action: LazyEvaluated<T>
) = Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.None, OverrideKind.None, emptyList())

@DslMarker
public annotation class ExtensionsDsl

@ExtensionsDsl
public class ExtendingDSL(private val pluginClass: String, private val extensionName: String) {

    public infix fun <T : Any> ExtensionPoint<T>.with(action: T): Extension<T, OrderingKind.None, OverrideKind.None> {
        return Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromInstance(action))
    }

    public infix fun <T : Any> ExtensionPoint<T>.providing(action: (DokkaContext) -> T): Extension<T, OrderingKind.None, OverrideKind.None> {
        return Extension(this, this@ExtendingDSL.pluginClass, extensionName, LazyEvaluated.fromRecipe(action))
    }

    public infix fun <T : Any, Override : OverrideKind> Extension<T, OrderingKind.None, Override>.order(
        block: OrderDsl.() -> Unit
    ): Extension<T, OrderingKind.ByDsl, Override> {
        return Extension(extensionPoint, pluginClass, extensionName, action, OrderingKind.ByDsl(block), override, conditions)
    }

    public infix fun <T : Any, Override : OverrideKind, Ordering: OrderingKind> Extension<T, Ordering, Override>.applyIf(
        condition: DokkaConfiguration.() -> Boolean
    ): Extension<T, Ordering, Override> {
        return Extension(extensionPoint, pluginClass, extensionName, action, ordering, override, conditions + condition)
    }

    public infix fun <T : Any, Override : OverrideKind, Ordering: OrderingKind> Extension<T, Ordering, Override>.override(
        overriden: List<Extension<T, *, *>>
    ): Extension<T, Ordering, OverrideKind.Present> {
        return Extension(extensionPoint, pluginClass, extensionName, action, ordering, OverrideKind.Present(overriden), conditions)
    }

    public infix fun <T : Any, Override : OverrideKind, Ordering: OrderingKind> Extension<T, Ordering, Override>.override(
        overriden: Extension<T, *, *>
    ): Extension<T, Ordering, OverrideKind.Present> {
        return this.override(listOf(overriden))
    }
}

@ExtensionsDsl
public class OrderDsl {
    internal val previous = mutableSetOf<Extension<*, *, *>>()
    internal val following = mutableSetOf<Extension<*, *, *>>()

    public fun after(vararg extensions: Extension<*, *, *>) {
        previous += extensions
    }

    public fun before(vararg extensions: Extension<*, *, *>) {
        following += extensions
    }
}
