/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.syntheticConstructors

public class OneParameter internal constructor(
    public val a: Int = 0
)

public class MultipleParameters internal constructor(
    public val a: Int = 0,
    public val b: Int = 0,
    public val c: Int = 0
)

public class MultipleConstructors internal constructor(
    public val a: Int = 0
) {
    private constructor(str: String) : this()
}

public class MixedParameters internal constructor(
    public val a: Int = 0,
    public val b: Int = 0,
    public val c: Int
)

public class SimpleClass(public val a: Int = 0)

public class PublishedConstructor @PublishedApi internal constructor(
    public val a: Int = 0
)

public abstract class ProtectedConstructor protected constructor(
    public val a: Int = 0
)

public class PrivateConstructor private constructor(
    public val a: Int = 0
)