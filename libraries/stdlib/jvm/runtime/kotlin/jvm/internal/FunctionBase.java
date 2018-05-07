/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.Function;

import java.io.Serializable;

public interface FunctionBase extends Function, Serializable {
    int getArity();
}
