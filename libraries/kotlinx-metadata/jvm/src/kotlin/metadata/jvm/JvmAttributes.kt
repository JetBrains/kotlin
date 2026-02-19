/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmAttributes")

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.internal.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import kotlin.metadata.jvm.internal.jvm
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags as JF
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags

/**
 * Indicates that the corresponding class has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmClass.hasAnnotationsInBytecode: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

/**
 * Indicates that the corresponding constructor has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmConstructor.hasAnnotationsInBytecode: Boolean by constructorBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

/**
 * Indicates that the corresponding function has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmFunction.hasAnnotationsInBytecode: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

/**
 * Indicates that the corresponding property has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmProperty.hasAnnotationsInBytecode: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

/**
 * Indicates that the corresponding property accessor has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmPropertyAccessorAttributes.hasAnnotationsInBytecode: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

/**
 * Indicates that the corresponding value parameter has at least one annotation in the JVM bytecode.
 *
 * Before annotations in metadata are enabled by default in the Kotlin compiler (https://youtrack.jetbrains.com/issue/KT-75736),
 * annotations are only generated in the JVM bytecode. The compiler writes and reads this flag to metadata as an optimization, to avoid
 * parsing class file one additional time when it's not needed.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmValueParameter.hasAnnotationsInBytecode: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.HAS_ANNOTATIONS))

// KmType and KmTypeParameter have annotations in it, and this flag for them is not written
// KmTypeAlias has both annotations and flag, but its value always corresponds to whether the annotations list is non-empty.


/**
 * Applicable to a property declared in an interface's companion object.
 * Indicates that its backing field is declared as a static
 * field in the interface. In Kotlin code, this usually happens if the property is annotated with [JvmField].
 *
 * Returns `false` if the property is not declared in a companion object of some interface.
 */
public var KmProperty.isMovedFromInterfaceCompanion: Boolean by BooleanFlagDelegate(
    KmProperty::jvmFlags,
    booleanFlag(JF.IS_MOVED_FROM_INTERFACE_COMPANION)
)

/**
 * Applicable to an interface compiled with `-jvm-default=enable` or `-jvm-default=no-compatibility`.
 * True if interface has method bodies in it, false if Kotlin compiler moved all interface method bodies into a nested `DefaultImpls`
 * class.
 *
 * Check [documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#compatibility-modes-for-default-methods) for more details.
 *
 * @see JvmDefaultWithCompatibility
 * @see JvmDefaultWithoutCompatibility
 */
public var KmClass.hasMethodBodiesInInterface: Boolean by BooleanFlagDelegate(
    KmClass::jvmFlags,
    booleanFlag(JF.IS_COMPILED_IN_JVM_DEFAULT_MODE)
)

/**
 * Indicates if an interface was compiled with `-jvm-default=enable`.
 *
 * In compatibility mode Kotlin/JVM compiler generates method bodies directly in the interface,
 * and also generates bridges in a nested `DefaultImpls` class.
 * Bridges are intended for use by already existing clients,
 * such as compiled Java code or Kotlin code compiled in the `-jvm-default=disable` mode.
 *
 * Also, can be a result of compiling interface with `@JvmDefaultWithCompatibility` annotation.
 * Check [documentation](https://kotlinlang.org/docs/java-to-kotlin-interop.html#compatibility-modes-for-default-methods) for more details.
 *
 * @see JvmDefaultWithCompatibility
 * @see JvmDefaultWithoutCompatibility
 */
public var KmClass.isCompiledInCompatibilityMode: Boolean by BooleanFlagDelegate(
    KmClass::jvmFlags,
    booleanFlag(JF.IS_COMPILED_IN_COMPATIBILITY_MODE)
)

private fun booleanFlag(f: Flags.BooleanFlagField): FlagImpl =
    FlagImpl(f.offset, f.bitWidth, 1)

private var KmProperty.jvmFlags: Int
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }


private var KmClass.jvmFlags: Int
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }
