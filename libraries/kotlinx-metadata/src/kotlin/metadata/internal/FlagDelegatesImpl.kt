/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR") // flags will become internal eventually

package kotlin.metadata.internal

import kotlin.metadata.*
import kotlin.metadata.internal.FlagImpl as Flag
import kotlin.enums.EnumEntries
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.metadata.deserialization.Flags.FlagField as ProtoFlagSet
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags
import org.jetbrains.kotlin.protobuf.Internal.EnumLite as ProtoEnumLite

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

internal fun classBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmClass::flags, flag)

internal fun functionBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmFunction::flags, flag)

internal fun constructorBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmConstructor::flags, flag)

internal fun propertyBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmProperty::flags, flag)

internal fun propertyAccessorBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmPropertyAccessorAttributes::flags, flag)

internal fun typeBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmType::flags, flag)

internal fun valueParameterBooleanFlag(flag: Flag) = BooleanFlagDelegate(KmValueParameter::flags, flag)

internal fun <Node> annotationsOn(flags: KMutableProperty1<Node, Int>) = BooleanFlagDelegate(flags, Flag(ProtoFlags.HAS_ANNOTATIONS))

