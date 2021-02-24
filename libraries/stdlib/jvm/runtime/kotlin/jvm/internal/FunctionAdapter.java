/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.Function;
import kotlin.SinceKotlin;

@SuppressWarnings("unused")
@SinceKotlin(version = "1.4")
public interface FunctionAdapter {
    Function<?> getFunctionDelegate();
}
