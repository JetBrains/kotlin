/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.*

private const val DEPRECATED_API_MESSAGE = "This API is deprecated without replacement"

@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
@OptIn(ExperimentalNativeApi::class)
public class Continuation0(block: () -> Unit,
                    private val invoker: CPointer<CFunction<(COpaquePointer?) -> Unit>>,
                    private val singleShot: Boolean = false): Function0<Unit> {

    private val stable = StableRef.create(block)

    public override operator fun invoke()  {
        invoker(stable.asCPointer())
        if (singleShot) {
            stable.dispose()
        }
    }

    public fun dispose() {
        assert(!singleShot)
        stable.dispose()
    }
}

@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
@OptIn(ExperimentalNativeApi::class)
public class Continuation1<T1>(
        block: (p1: T1) -> Unit,
        private val invoker: CPointer<CFunction<(COpaquePointer?) -> Unit>>,
        private val singleShot: Boolean = false) : Function1<T1, Unit> {

    private val stable = StableRef.create(block)

    public override operator fun invoke(p1: T1) {
        val args = StableRef.create(Pair(stable, p1))
        try {
            invoker(args.asCPointer())
        } finally {
            args.dispose()
        }
        if (singleShot) {
            stable.dispose()
        }
    }

    public fun dispose() {
        assert(!singleShot)
        stable.dispose()
    }
}

@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
@OptIn(ExperimentalNativeApi::class)
public class Continuation2<T1, T2>(
        block: (p1: T1, p2: T2) -> Unit,
        private val invoker: CPointer<CFunction<(COpaquePointer?) -> Unit>>,
        private val singleShot: Boolean = false) : Function2<T1, T2, Unit> {

    private val stable = StableRef.create(block)

    public override operator fun invoke(p1: T1, p2: T2) {
        val args = StableRef.create(Triple(stable, p1, p2))
        try {
            invoker(args.asCPointer())
        } finally {
            args.dispose()
        }
        if (singleShot) {
            stable.dispose()
        }
    }

    public fun dispose() {
        assert(!singleShot)
        stable.dispose()
    }
}


@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
public fun COpaquePointer.callContinuation0() {
    val single = this.asStableRef<() -> Unit>()
    single.get()()
}

@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
public fun <T1> COpaquePointer.callContinuation1() {
    val pair = this.asStableRef<Pair<StableRef<(T1) -> Unit>, T1>>().get()
    pair.first.get()(pair.second)
}

@Deprecated(DEPRECATED_API_MESSAGE, level = DeprecationLevel.WARNING)
public fun <T1, T2> COpaquePointer.callContinuation2() {
    val triple = this.asStableRef<Triple<StableRef<(T1, T2) -> Unit>, T1, T2>>().get()
    triple.first.get()(triple.second, triple.third)
}
