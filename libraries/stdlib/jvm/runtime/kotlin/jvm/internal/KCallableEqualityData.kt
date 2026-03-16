/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

/**
 * Provides data used for equality check for most of [kotlin.reflect.KCallable]s.
 */
@SinceKotlin("2.4")
public interface KCallableEqualityData {
    public val name: String
    public val signature: String
    public val owner: Any?
    public val rawBoundReceiver: Any?
}
