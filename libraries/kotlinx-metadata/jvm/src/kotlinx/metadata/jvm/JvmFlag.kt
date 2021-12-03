/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import org.jetbrains.kotlin.metadata.deserialization.Flags as F
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags as JF

/**
 * JVM-specific flags in addition to common flags declared in [Flag].
 *
 * @see Flag
 * @see Flags
 */
object JvmFlag {
    /**
     * JVM-specific property flags in addition to common property flags declared in [Flag.Property].
     */
    object Property {
        /**
         * Applied to a property declared in an interface's companion object, signifies that its backing field is declared as a static
         * field in the interface. In Kotlin code, this usually happens if the property is annotated with [JvmField].
         *
         * Has no effect if the property is not declared in a companion object of some interface.
         */
        @JvmField
        val IS_MOVED_FROM_INTERFACE_COMPANION = booleanFlag(JF.IS_MOVED_FROM_INTERFACE_COMPANION)
    }

    /**
     * JVM-specific class flags in addition to common class flags declared in [Flag.Class].
     */
    object Class {
        /**
         * Applied to an interface compiled with -Xjvm-default=all or all-compatibility.
         *
         * Without this flag or a `@JvmDefault` annotation on individual interface methods
         * the Kotlin compiler moves all interface method bodies into a nested `DefaultImpls`
         * class.
         */
        @JvmField
        val HAS_METHOD_BODIES_IN_INTERFACE = booleanFlag(JF.IS_COMPILED_IN_JVM_DEFAULT_MODE)

        /**
         * Applied to an interface compiled with -Xjvm-default=all-compatibility.
         *
         * In compatibility mode we generate method bodies directly in the interface,
         * but we also generate bridges in a nested `DefaultImpls` class for use by
         * clients compiled without all-compatibility.
         */
        @JvmField
        val IS_COMPILED_IN_COMPATIBILITY_MODE = booleanFlag(JF.IS_COMPILED_IN_COMPATIBILITY_MODE)
    }

    private fun booleanFlag(f: F.BooleanFlagField): Flag =
        Flag(f.offset, f.bitWidth, 1)
}
