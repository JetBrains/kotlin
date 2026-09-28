/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI

@InternalDokkaApi
public interface KotlinToJavaService {
    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.Int -> java.lang.Integer
     * kotlin.Int.Companion -> kotlin.jvm.internal.IntCompanionObject
     * kotlin.Nothing -> java.lang.Void
     * kotlin.IntArray -> null
     * kotlin.Function3 -> kotlin.jvm.functions.Function3
     * kotlin.coroutines.SuspendFunction3 -> kotlin.jvm.functions.Function4
     * kotlin.Function42 -> kotlin.jvm.functions.FunctionN
     * kotlin.coroutines.SuspendFunction42 -> kotlin.jvm.functions.FunctionN
     * kotlin.reflect.KFunction3 -> kotlin.reflect.KFunction
     * kotlin.reflect.KSuspendFunction3 -> kotlin.reflect.KFunction
     * kotlin.reflect.KFunction42 -> kotlin.reflect.KFunction
     * kotlin.reflect.KSuspendFunction42 -> kotlin.reflect.KFunction
     */
    public fun findAsJava(kotlinDri: DRI): DRI?
}
