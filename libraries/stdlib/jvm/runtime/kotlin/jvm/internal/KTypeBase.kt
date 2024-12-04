/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import java.lang.reflect.Type
import kotlin.reflect.KType

@SinceKotlin("1.4")
public interface KTypeBase : KType {
    public val javaType: Type?
}
