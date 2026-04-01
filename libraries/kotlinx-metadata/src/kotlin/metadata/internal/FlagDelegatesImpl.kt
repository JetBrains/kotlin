/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.metadata.internal

import kotlin.enums.EnumEntries
import kotlin.metadata.*
import kotlin.metadata.internal.FlagImpl
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags
import org.jetbrains.kotlin.metadata.deserialization.Flags.FlagField as ProtoFlagSet
import org.jetbrains.kotlin.protobuf.Internal.EnumLite as ProtoEnumLite
import kotlin.metadata.internal.FlagImpl as Flag

internal class EnumFlagDelegate<Node, E : Enum<E>>(
    val flags: KMutableProperty1<Node, Int>,
    private val protoSet: ProtoFlagSet<out ProtoEnumLite>,
    private val entries: EnumEntries<E>,
    private val flagValues: List<Flag>
) {
    // Pre-built permutation ProtoEnum <> E to allow reordering of enum entries?
    // Concern: if new enum values are added to metadata proto, everything (including existing flags) will break
    operator fun getValue(thisRef: Node, property: KProperty<*>): E = entries[protoSet.get(flags.get(thisRef)).number]

    operator fun setValue(thisRef: Node, property: KProperty<*>, value: E) {
        flags.set(thisRef, flagValues[value.ordinal] + flags.get(thisRef))
    }
}

// Public in internal package - for reuse in JvmFlags
public class BooleanFlagDelegate<Node>(private val flags: KMutableProperty1<Node, Int>, private val flag: Flag) {
    private val mask: Int

    init {
        require(flag.bitWidth == 1 && flag.value == 1) { "BooleanFlagDelegate can work only with boolean flags (bitWidth = 1 and value = 1), but $flag was passed" }
        mask = 1 shl flag.offset
    }

    public operator fun getValue(thisRef: Node, property: KProperty<*>): Boolean = flag(flags.get(thisRef))

    public operator fun setValue(thisRef: Node, property: KProperty<*>, value: Boolean) {
        val newValue = if (value) flags.get(thisRef) or mask else flags.get(thisRef) and mask.inv()
        flags.set(thisRef, newValue)
    }
}


internal fun <Node> visibilityDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.VISIBILITY, Visibility.entries, Visibility.entries.map { it.flag })

internal fun <Node> modalityDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.MODALITY, Modality.entries, Modality.entries.map { it.flag })

internal fun <Node> memberKindDelegate(flags: KMutableProperty1<Node, Int>) =
    EnumFlagDelegate(flags, ProtoFlags.MEMBER_KIND, MemberKind.entries, MemberKind.entries.map { it.flag })

@ExperimentalMustUseStatus
internal fun <Node> returnValueStatusDelegate(flags: KMutableProperty1<Node, Int>, protoSet: ProtoFlagSet<out ProtoEnumLite>) =
    EnumFlagDelegate(flags, protoSet, ReturnValueStatus.entries, ReturnValueStatus.entries.map { FlagImpl(protoSet, it.ordinal) })

public fun classBooleanFlag(flag: Flag): BooleanFlagDelegate<KmClass> =
    BooleanFlagDelegate(KmClass::flags, flag)

public fun functionBooleanFlag(flag: Flag): BooleanFlagDelegate<KmFunction> =
    BooleanFlagDelegate(KmFunction::flags, flag)

public fun constructorBooleanFlag(flag: Flag): BooleanFlagDelegate<KmConstructor> =
    BooleanFlagDelegate(KmConstructor::flags, flag)

public fun propertyBooleanFlag(flag: Flag): BooleanFlagDelegate<KmProperty> =
    BooleanFlagDelegate(KmProperty::flags, flag)

public fun propertyAccessorBooleanFlag(flag: Flag): BooleanFlagDelegate<KmPropertyAccessorAttributes> =
    BooleanFlagDelegate(KmPropertyAccessorAttributes::flags, flag)

public fun valueParameterBooleanFlag(flag: Flag): BooleanFlagDelegate<KmValueParameter> =
    BooleanFlagDelegate(KmValueParameter::flags, flag)

internal fun typeAliasBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmTypeAlias::flags, flag)

internal fun typeBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmType::flags, flag)

// Used for kotlin-metadata-jvm tests:
public fun _flagAccess(kmClass: KmClass): Int = kmClass.flags

public fun _flagAccess(kmFunc: KmFunction): Int = kmFunc.flags

public fun _flagAccess(kmType: KmType): Int = kmType.flags

public fun _flagAccess(kmConstructor: KmConstructor): Int = kmConstructor.flags
