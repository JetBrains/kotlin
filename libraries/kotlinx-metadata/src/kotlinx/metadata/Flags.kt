/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("FlagsKt")

package kotlinx.metadata

/**
 * Declaration flags are represented as bitmasks of this type.
 *
 * @see Flag
 */
typealias Flags = Int

/**
 * Combines several flags into an integer bitmask.
 *
 * Note that in case several mutually exclusive flags are passed (for example, several visibility flags), the resulting bitmask will
 * hold the value of the latest flag. For example, `flagsOf(Flag.IS_PRIVATE, Flag.IS_PUBLIC, Flag.IS_INTERNAL)` is the same as
 * `flagsOf(Flag.IS_INTERNAL)`.
 */
fun flagsOf(vararg flags: Flag): Flags =
    flags.fold(0) { acc, flag -> flag + acc }
