/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

public interface WithChildren<out T> {
    public val children: List<T>
}

public inline fun <reified T> WithChildren<*>.firstChildOfTypeOrNull(): T? =
    children.filterIsInstance<T>().firstOrNull()

public inline fun <reified T> WithChildren<*>.firstChildOfTypeOrNull(predicate: (T) -> Boolean): T? =
    children.filterIsInstance<T>().firstOrNull(predicate)

public inline fun <reified T> WithChildren<*>.firstChildOfType(): T =
    children.filterIsInstance<T>().first()

public inline fun <reified T> WithChildren<*>.childrenOfType(): List<T> =
    children.filterIsInstance<T>()

public inline fun <reified T> WithChildren<*>.firstChildOfType(predicate: (T) -> Boolean): T =
    children.filterIsInstance<T>().first(predicate)

public inline fun <reified T> WithChildren<WithChildren<*>>.firstMemberOfType(): T where T : WithChildren<*> {
    return withDescendants().filterIsInstance<T>().first()
}

public inline fun <reified T> WithChildren<WithChildren<*>>.firstMemberOfType(
    predicate: (T) -> Boolean
): T where T : WithChildren<*> = withDescendants().filterIsInstance<T>().first(predicate)


public inline fun <reified T> WithChildren<WithChildren<*>>.firstMemberOfTypeOrNull(): T? where T : WithChildren<*> {
    return withDescendants().filterIsInstance<T>().firstOrNull()
}

public fun <T> T.withDescendants(): Sequence<T> where T : WithChildren<T> {
    return sequence {
        yield(this@withDescendants)
        children.forEach { child ->
            yieldAll(child.withDescendants())
        }
    }
}

@JvmName("withDescendantsProjection")
public fun WithChildren<*>.withDescendants(): Sequence<Any?> {
    return sequence {
        yield(this@withDescendants)
        children.forEach { child ->
            if (child is WithChildren<*>) {
                yieldAll(child.withDescendants())
            }
        }
    }
}

@JvmName("withDescendantsAny")
public fun WithChildren<Any>.withDescendants(): Sequence<Any> {
    return sequence {
        yield(this@withDescendants)
        children.forEach { child ->
            if (child is WithChildren<*>) {
                yieldAll(child.withDescendants().filterNotNull())
            }
        }
    }
}

public fun <T> T.dfs(predicate: (T) -> Boolean): T? where T : WithChildren<T> = if (predicate(this)) {
    this
} else {
    children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
}

public fun <T : WithChildren<T>> T.asPrintableTree(
    nodeNameBuilder: Appendable.(T) -> Unit = { append(it.toString()) }
): String {
    fun Appendable.append(element: T, ownPrefix: String, childPrefix: String) {
        append(ownPrefix)
        nodeNameBuilder(element)
        appendLine()
        element.children.takeIf(Collection<*>::isNotEmpty)?.also { children ->
            val newOwnPrefix = "$childPrefix├─ "
            val lastOwnPrefix = "$childPrefix└─ "
            val newChildPrefix = "$childPrefix│  "
            val lastChildPrefix = "$childPrefix   "
            children.forEachIndexed { n, e ->
                if (n != children.lastIndex) append(e, newOwnPrefix, newChildPrefix)
                else append(e, lastOwnPrefix, lastChildPrefix)
            }
        }
    }

    return buildString { append(this@asPrintableTree, "", "") }
}
