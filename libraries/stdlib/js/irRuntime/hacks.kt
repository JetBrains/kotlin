/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.coroutines.experimental.SequenceBuilder

// TODO: Ignore FunctionN interfaces

public interface Function0<out R> : Function<R> {
    public operator fun invoke(): R
}

public interface Function1<in P1, out R> : Function<R> {
    public operator fun invoke(p1: P1): R
}

public interface Function2<in P1, in P2, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2): R
}

public interface Function3<in P1, in P2, in P3, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2, p3: P3): R
}

public inline fun <reified T> arrayOfNulls(size: Int): Array<T?> = js("[]")  // FIXME: Implement

internal inline fun <T> buildSequence(noinline builderAction: suspend SequenceBuilder<T>.() -> Unit): Sequence<T> =
    kotlin.coroutines.experimental.buildSequence(builderAction)

internal inline fun <T> buildIterator(noinline builderAction: suspend SequenceBuilder<T>.() -> Unit): Iterator<T> =
    kotlin.coroutines.experimental.buildIterator(builderAction)

internal inline fun <T> sequence(noinline builderAction: suspend SequenceBuilder<T>.() -> Unit): Sequence<T> =
    kotlin.coroutines.experimental.buildSequence(builderAction)

internal inline fun <T> iterator(noinline builderAction: suspend SequenceBuilder<T>.() -> Unit): Iterator<T> =
    kotlin.coroutines.experimental.buildIterator(builderAction)
