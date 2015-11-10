/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.incremental

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.jps.incremental.ProtoCompareGenerated.ProtoBufClassKind
import org.jetbrains.kotlin.jps.incremental.ProtoCompareGenerated.ProtoBufPackageKind
import org.jetbrains.kotlin.jps.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.Deserialization
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.HashSetUtil
import java.util.*

public sealed class DifferenceKind() {
    public object NONE: DifferenceKind()
    public object CLASS_SIGNATURE: DifferenceKind()
    public class MEMBERS(val names: Collection<String>): DifferenceKind()
}

public fun difference(oldData: ProtoMapValue, newData: ProtoMapValue): DifferenceKind {
    if (oldData.isPackageFacade != newData.isPackageFacade) return DifferenceKind.CLASS_SIGNATURE

    val differenceObject =
            if (oldData.isPackageFacade) DifferenceCalculatorForPackageFacade(oldData, newData) else DifferenceCalculatorForClass(oldData, newData)

    return differenceObject.difference()
}

internal val MessageLite.isPrivate: Boolean
    get() = Visibilities.isPrivate(Deserialization.visibility(
            when (this) {
                is ProtoBuf.Constructor -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Function -> Flags.VISIBILITY.get(flags)
                is ProtoBuf.Property -> Flags.VISIBILITY.get(flags)
                else -> error("Unknown message: $this")
            }))

private fun MessageLite.name(nameResolver: NameResolver): String {
    return when (this) {
        is ProtoBuf.Constructor -> "<init>"
        is ProtoBuf.Function -> nameResolver.getString(name)
        is ProtoBuf.Property -> nameResolver.getString(name)
        else -> error("Unknown message: $this")
    }
}

internal fun List<MessageLite>.names(nameResolver: NameResolver): List<String> = map { it.name(nameResolver) }

private abstract class DifferenceCalculator() {
    protected abstract val oldNameResolver: NameResolver
    protected abstract val newNameResolver: NameResolver

    protected val compareObject by lazy { ProtoCompareGenerated(oldNameResolver, newNameResolver) }

    abstract fun difference(): DifferenceKind

    protected fun membersOrNone(names: Collection<String>): DifferenceKind = if (names.isEmpty()) DifferenceKind.NONE else DifferenceKind.MEMBERS(names)

    protected fun calcDifferenceForMembers(oldList: List<MessageLite>, newList: List<MessageLite>): Collection<String> {
        val result = hashSetOf<String>()

        val oldMap =
                oldList.groupBy { it.getHashCode({ compareObject.oldGetIndexOfString(it) }, { compareObject.oldGetIndexOfClassId(it) }) }
        val newMap =
                newList.groupBy { it.getHashCode({ compareObject.newGetIndexOfString(it) }, { compareObject.newGetIndexOfClassId(it) }) }

        val hashes = oldMap.keySet() + newMap.keySet()
        for (hash in hashes) {
            val oldMembers = oldMap[hash]
            val newMembers = newMap[hash]

            val differentMembers = when {
                newMembers == null -> oldMembers!!.names(compareObject.oldNameResolver)
                oldMembers == null -> newMembers.names(compareObject.newNameResolver)
                else -> calcDifferenceForEqualHashes(oldMembers, newMembers)
            }
            result.addAll(differentMembers)
        }

        return result
    }

    private fun calcDifferenceForEqualHashes(
            oldList: List<MessageLite>,
            newList: List<MessageLite>
    ): Collection<String> {
        val result = hashSetOf<String>()
        val newSet = HashSet(newList)

        oldList.forEach { oldMember ->
            val newMember = newSet.firstOrNull { compareObject.checkEquals(oldMember, it) }
            if (newMember != null) {
                newSet.remove(newMember)
            }
            else {
                result.add(oldMember.name(compareObject.oldNameResolver))
            }
        }

        newSet.forEach { newMember ->
            result.add(newMember.name(compareObject.newNameResolver))
        }

        return result
    }

    protected fun calcDifferenceForNames(
            oldList: List<Int>,
            newList: List<Int>
    ): Collection<String> {
        val oldNames = oldList.map { compareObject.oldNameResolver.getString(it) }.toSet()
        val newNames = newList.map { compareObject.newNameResolver.getString(it) }.toSet()
        return HashSetUtil.symmetricDifference(oldNames, newNames)
    }

    private fun MessageLite.getHashCode(stringIndexes: (Int) -> Int, fqNameIndexes: (Int) -> Int): Int {
        return when (this) {
            is ProtoBuf.Constructor -> hashCode(stringIndexes, fqNameIndexes)
            is ProtoBuf.Function -> hashCode(stringIndexes, fqNameIndexes)
            is ProtoBuf.Property -> hashCode(stringIndexes, fqNameIndexes)
            else -> error("Unknown message: $this")
        }
    }

    private fun ProtoCompareGenerated.checkEquals(old: MessageLite, new: MessageLite): Boolean {
        return when {
            old is ProtoBuf.Constructor && new is ProtoBuf.Constructor -> checkEquals(old, new)
            old is ProtoBuf.Function && new is ProtoBuf.Function -> checkEquals(old, new)
            old is ProtoBuf.Property && new is ProtoBuf.Property -> checkEquals(old, new)
            else -> error("Unknown message: $this")
        }
    }
}

private class DifferenceCalculatorForClass(oldData: ProtoMapValue, newData: ProtoMapValue) : DifferenceCalculator() {
    companion object {
        private val CLASS_SIGNATURE_ENUMS = EnumSet.of(
                ProtoBufClassKind.FLAGS,
                ProtoBufClassKind.FQ_NAME,
                ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoBufClassKind.CLASS_ANNOTATION_LIST
        )
    }

    val oldClassData = JvmProtoBufUtil.readClassDataFrom(oldData.bytes, oldData.strings)
    val newClassData = JvmProtoBufUtil.readClassDataFrom(newData.bytes, newData.strings)

    val oldProto = oldClassData.classProto
    val newProto = newClassData.classProto

    override val oldNameResolver = oldClassData.nameResolver
    override val newNameResolver = newClassData.nameResolver

    val diff = compareObject.difference(oldProto, newProto)

    override fun difference(): DifferenceKind {
        if (diff.isEmpty()) return DifferenceKind.NONE

        CLASS_SIGNATURE_ENUMS.forEach { if (it in diff) return DifferenceKind.CLASS_SIGNATURE }

        return membersOrNone(getChangedMembersNames())
    }

    private fun getChangedMembersNames(): Set<String> {
        val names = hashSetOf<String>()

        fun Int.oldToNames() = names.add(oldNameResolver.getString(this))
        fun Int.newToNames() = names.add(newNameResolver.getString(this))

        fun calcDifferenceForNonPrivateMembers(members: (ProtoBuf.Class) -> List<MessageLite>): Collection<String> {
            val oldMembers = members(oldProto).filterNot { it.isPrivate }
            val newMembers = members(newProto).filterNot { it.isPrivate }
            return calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            when (kind!!) {
                ProtoBufClassKind.COMPANION_OBJECT_NAME -> {
                    if (oldProto.hasCompanionObjectName()) oldProto.companionObjectName.oldToNames()
                    if (newProto.hasCompanionObjectName()) newProto.companionObjectName.newToNames()
                }
                ProtoBufClassKind.NESTED_CLASS_NAME_LIST ->
                    names.addAll(calcDifferenceForNames(oldProto.nestedClassNameList, newProto.nestedClassNameList))
                ProtoBufClassKind.CONSTRUCTOR_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getConstructorList))
                ProtoBufClassKind.FUNCTION_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getFunctionList))
                ProtoBufClassKind.PROPERTY_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Class::getPropertyList))
                ProtoBufClassKind.ENUM_ENTRY_LIST ->
                    names.addAll(calcDifferenceForNames(oldProto.enumEntryList, newProto.enumEntryList))
                ProtoBufClassKind.TYPE_TABLE -> {
                    // TODO
                }
                ProtoBufClassKind.FLAGS,
                ProtoBufClassKind.FQ_NAME,
                ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoBufClassKind.CLASS_ANNOTATION_LIST ->
                    throw IllegalArgumentException("Unexpected kind: $kind")
                else ->
                    throw IllegalArgumentException("Unsupported kind: $kind")
            }
        }
        return names
    }
}

private class DifferenceCalculatorForPackageFacade(oldData: ProtoMapValue, newData: ProtoMapValue) : DifferenceCalculator() {
    val oldPackageData = JvmProtoBufUtil.readPackageDataFrom(oldData.bytes, oldData.strings)
    val newPackageData = JvmProtoBufUtil.readPackageDataFrom(newData.bytes, newData.strings)

    val oldProto = oldPackageData.packageProto
    val newProto = newPackageData.packageProto

    override val oldNameResolver = oldPackageData.nameResolver
    override val newNameResolver = newPackageData.nameResolver

    val diff = compareObject.difference(oldProto, newProto)

    override fun difference(): DifferenceKind {
        if (diff.isEmpty()) return DifferenceKind.NONE

        return membersOrNone(getChangedMembersNames())
    }

    private fun getChangedMembersNames(): Set<String> {
        val names = hashSetOf<String>()

        fun calcDifferenceForNonPrivateMembers(members: (ProtoBuf.Package) -> List<MessageLite>): Collection<String> {
            val oldMembers = members(oldProto).filterNot { it.isPrivate }
            val newMembers = members(newProto).filterNot { it.isPrivate }
            return calcDifferenceForMembers(oldMembers, newMembers)
        }

        for (kind in diff) {
            when (kind!!) {
                ProtoBufPackageKind.FUNCTION_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getFunctionList))
                ProtoBufPackageKind.PROPERTY_LIST ->
                    names.addAll(calcDifferenceForNonPrivateMembers(ProtoBuf.Package::getPropertyList))
                ProtoBufPackageKind.TYPE_TABLE -> {
                    // TODO
                }
                else ->
                    throw IllegalArgumentException("Unsupported kind: $kind")
            }
        }

        return names
    }
}
