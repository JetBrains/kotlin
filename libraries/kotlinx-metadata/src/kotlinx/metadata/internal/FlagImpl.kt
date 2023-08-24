/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.internal

import org.jetbrains.kotlin.metadata.deserialization.Flags as F

public class FlagImpl(internal val offset: Int, internal val bitWidth: Int, internal val value: Int) {
    @IgnoreInApiDump
    internal constructor(field: F.FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    @IgnoreInApiDump
    internal constructor(field: F.BooleanFlagField) : this(field, 1)

    internal operator fun plus(flags: Int): Int =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)

    public operator fun invoke(flags: Int): Boolean = (flags ushr offset) and ((1 shl bitWidth) - 1) == value
}
