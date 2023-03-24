/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.KmClass
import kotlinx.metadata.KmProperty
import kotlinx.metadata.internal.BooleanFlagDelegate
import kotlinx.metadata.jvm.JvmFlag.booleanFlag
import kotlinx.metadata.jvm.internal.jvm
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags as JF

/**
 * Applied to a property declared in an interface's companion object, signifies that its backing field is declared as a static
 * field in the interface. In Kotlin code, this usually happens if the property is annotated with [JvmField].
 *
 * Has no effect if the property is not declared in a companion object of some interface.
 */
var KmProperty.isMovedFromInterfaceCompanion by BooleanFlagDelegate(KmProperty::jvmFlags, booleanFlag(JF.IS_MOVED_FROM_INTERFACE_COMPANION))

/**
 * Applied to an interface compiled with -Xjvm-default=all or all-compatibility.
 *
 * Without this flag or a `@JvmDefault` annotation on individual interface methods
 * the Kotlin compiler moves all interface method bodies into a nested `DefaultImpls`
 * class.
 */
var KmClass.hasMethodBodiesInInterface by BooleanFlagDelegate(KmClass::jvmFlags, booleanFlag(JF.IS_COMPILED_IN_JVM_DEFAULT_MODE))

/**
 * Applied to an interface compiled with -Xjvm-default=all-compatibility.
 *
 * In compatibility mode we generate method bodies directly in the interface,
 * but we also generate bridges in a nested `DefaultImpls` class for use by
 * clients compiled without all-compatibility.
 */
var KmClass.isCompiledInCompatibilityMode by BooleanFlagDelegate(KmClass::jvmFlags, booleanFlag(JF.IS_COMPILED_IN_COMPATIBILITY_MODE))
