/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.internal.FlagImpl
import org.jetbrains.kotlin.metadata.ProtoBuf.Visibility as ProtoVisibility
import org.jetbrains.kotlin.metadata.deserialization.Flags as F

fun Flag(field: F.FlagField<*>, value: Int) = FlagImpl(field.offset, field.bitWidth, value)

fun Flag(field: F.BooleanFlagField) = Flag(field, 1)

// Reduced copy of old Flag.kt for testing purposes.
class Flag private constructor() {

    companion object Common {
        /**
         * A visibility flag, signifying that the corresponding declaration is `internal`.
         */
        val IS_INTERNAL = Flag(F.VISIBILITY, ProtoVisibility.INTERNAL_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is `private`.
         */
        @JvmField
        val IS_PRIVATE = Flag(F.VISIBILITY, ProtoVisibility.PRIVATE_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is `public`.
         */
        @JvmField
        val IS_PUBLIC = Flag(F.VISIBILITY, ProtoVisibility.PUBLIC_VALUE)
    }

    object Class {
        /**
         * Signifies that the corresponding class is `data`.
         */
        @JvmField
        val IS_DATA = Flag(F.IS_DATA)
    }

    object Constructor {
        /**
         * Signifies that the corresponding constructor has non-stable parameter names, i.e. cannot be called with named arguments.
         */
        @JvmField
        val HAS_NON_STABLE_PARAMETER_NAMES = Flag(F.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES)
    }

    object Function {
        /**
         * Signifies that the corresponding function is `operator`.
         */
        @JvmField
        val IS_OPERATOR = Flag(F.IS_OPERATOR)


        /**
         * Signifies that the corresponding function has non-stable parameter names, i.e. cannot be called with named arguments.
         */
        @JvmField
        val HAS_NON_STABLE_PARAMETER_NAMES = Flag(F.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES)
    }

    object Type {
        /**
         * Signifies that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
         */
        @JvmField
        val IS_NULLABLE = FlagImpl(0, 1, 1)

    }
}
