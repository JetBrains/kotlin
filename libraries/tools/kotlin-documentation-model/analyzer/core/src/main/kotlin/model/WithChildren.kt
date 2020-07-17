package org.jetbrains.dokka.model

interface WithChildren<out T> {
    val children: List<T>
}

inline fun <reified T> WithChildren<*>.firstChildOfTypeOrNull(): T? =
    children.filterIsInstance<T>().firstOrNull()

inline fun <reified T> WithChildren<*>.firstChildOfTypeOrNull(predicate: (T) -> Boolean): T? =
    children.filterIsInstance<T>().firstOrNull(predicate)

inline fun <reified T> WithChildren<*>.firstChildOfType(): T =
    children.filterIsInstance<T>().first()

inline fun <reified T> WithChildren<*>.childrenOfType(): List<T> =
    children.filterIsInstance<T>()

inline fun <reified T> WithChildren<*>.firstChildOfType(predicate: (T) -> Boolean): T =
    children.filterIsInstance<T>().first(predicate)

inline fun <reified T> WithChildren<WithChildren<*>>.firstMemberOfType(): T where T : WithChildren<*> {
    return withDescendants().filterIsInstance<T>().first()
}

inline fun <reified T> WithChildren<WithChildren<*>>.firstMemberOfTypeOrNull(): T? where T : WithChildren<*> {
    return withDescendants().filterIsInstance<T>().firstOrNull()
}

fun <T> T.withDescendants(): Sequence<T> where T : WithChildren<T> {
    return sequence {
        yield(this@withDescendants)
        children.forEach { child ->
            yieldAll(child.withDescendants())
        }
    }
}

@JvmName("withDescendantsProjection")
fun WithChildren<*>.withDescendants(): Sequence<Any?> {
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
fun WithChildren<Any>.withDescendants(): Sequence<Any> {
    return sequence {
        yield(this@withDescendants)
        children.forEach { child ->
            if (child is WithChildren<*>) {
                yieldAll(child.withDescendants().filterNotNull())
            }
        }
    }
}

fun <T> T.dfs(predicate: (T) -> Boolean): T? where T : WithChildren<T> = if (predicate(this)) {
    this
} else {
    children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
}
