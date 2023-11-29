/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR") // flags will become internal eventually
@file:JvmName("JvmAttributes")

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.internal.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags as JF

/**
 * Applicable to a property declared in an interface's companion object.
 * Indicates that its backing field is declared as a static
 * field in the interface. In Kotlin code, this usually happens if the property is annotated with [JvmField].
 *
 * Returns `false` if the property is not declared in a companion object of some interface.
 */
public var KmProperty.isMovedFromInterfaceCompanion: Boolean by BooleanFlagDelegate(KmProperty::jvmFlags, booleanFlag(JF.IS_MOVED_FROM_INTERFACE_COMPANION))

/**
 * Applicable to an interface compiled with -Xjvm-default=all or all-compatibility.
 * True if interface has method bodies in it, false if Kotlin compiler moved all interface method bodies into a nested `DefaultImpls`
 * class.
 *
 * Method bodies are also present in interface method if it has `@JvmDefault` annotation (now deprecated).
 *
 * Check [documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#compatibility-modes-for-default-methods) for more details.
 *
 * @see JvmDefault
 * @see JvmDefaultWithCompatibility
 * @see JvmDefaultWithoutCompatibility
 */
public var KmClass.hasMethodBodiesInInterface: Boolean by BooleanFlagDelegate(KmClass::jvmFlags, booleanFlag(JF.IS_COMPILED_IN_JVM_DEFAULT_MODE))

/**
 * Indicates if an interface was compiled with -Xjvm-default=all-compatibility.
 *
 * In compatibility mode Kotlin/JVM compiler generates method bodies directly in the interface,
 * and also generates bridges in a nested `DefaultImpls` class.
 * Bridges are intended for use by already existing clients,
 * such as compiled Java code or Kotlin code compiled without all/all-compatibility setting.
 *
 * Also, can be a result of compiling interface with `@JvmDefaultWithCompatibility` annotation.
 * Check [documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#compatibility-modes-for-default-methods) for more details.
 *
 * @see JvmDefaultWithCompatibility
 * @see JvmDefaultWithoutCompatibility
 */
public var KmClass.isCompiledInCompatibilityMode: Boolean by BooleanFlagDelegate(KmClass::jvmFlags, booleanFlag(JF.IS_COMPILED_IN_COMPATIBILITY_MODE))

private fun booleanFlag(f: Flags.BooleanFlagField): FlagImpl =
    FlagImpl(f.offset, f.bitWidth, 1)
