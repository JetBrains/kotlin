/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import org.jetbrains.kotlin.metadata.deserialization.Flags.BooleanFlagField
import org.jetbrains.kotlin.metadata.deserialization.Flags.FlagField

/**
 * Represents a boolean flag that is either present or not in a Kotlin declaration.
 *
 * @see Flags
 */
class MetadataFlag internal constructor(
    private val offset: Int,
    private val bitWidth: Int,
    private val value: Int
) {
    internal constructor(field: FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    internal constructor(field: BooleanFlagField) : this(field, 1)

    internal operator fun plus(flags: Int): Int =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)

    /**
     * Checks whether the flag is present in the given bitmask.
     */
    operator fun invoke(flags: Int): Boolean =
        (flags ushr offset) and ((1 shl bitWidth) - 1) == value
}
