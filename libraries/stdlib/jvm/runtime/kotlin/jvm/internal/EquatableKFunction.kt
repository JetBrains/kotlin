/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.KFunction

@SinceKotlin("2.4")
public interface EquatableKFunction<out R> : KFunction<R>, EquatableKCallable