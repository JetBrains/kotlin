/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.internalConstructors

public class OneParameter internal constructor(
    public val a: Int = 0
)

public class MultipleParameters internal constructor(
    public val a: Int = 0,
    public val b: Int = 0,
    public val c: Int = 0
)

public class MixedParameters internal constructor(
    public val a: Int = 0,
    public val b: Int = 0,
    public val c: Int
)