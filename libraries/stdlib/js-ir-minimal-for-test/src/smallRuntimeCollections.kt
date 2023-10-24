/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

@SinceKotlin("1.4")
@library("arrayEquals")
public infix fun <T> Array<out T>?.contentEquals(other: Array<out T>?): Boolean {
    definedExternally
}

public interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}
public interface MutableIterable<out T> : Iterable<T> {
    override fun iterator(): MutableIterator<T>
}
public interface Collection<out E> : Iterable<E> {
    public val size: Int
}
public interface MutableCollection<E> : Collection<E>, MutableIterable<E>
public interface List<out E> : Collection<E>
public interface MutableList<E> : List<E>, MutableCollection<E>
public interface Set<out E> : Collection<E>
public interface MutableSet<E> : Set<E>, MutableCollection<E>
public interface Map<K, out V> {
    public interface Entry<out K, out V> {
        public val key: K
        public val value: V
    }
}
public interface MutableMap<K, V> : Map<K, V> {
    public interface MutableEntry<K, V> : Map.Entry<K, V> {
        public fun setValue(newValue: V): V
    }
}
