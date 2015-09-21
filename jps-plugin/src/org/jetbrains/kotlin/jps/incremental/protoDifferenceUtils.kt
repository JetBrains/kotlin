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

import org.jetbrains.kotlin.descriptors.Visibilities
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

data class ProtoMapValue(val isPackageFacade: Boolean, val bytes: ByteArray, val strings: Array<String>)

public fun difference(oldData: ProtoMapValue, newData: ProtoMapValue): DifferenceKind {
    if (oldData.isPackageFacade != newData.isPackageFacade) return DifferenceKind.CLASS_SIGNATURE

    val differenceObject =
            if (oldData.isPackageFacade) DifferenceCalculatorForPackageFacade(oldData, newData) else DifferenceCalculatorForClass(oldData, newData)

    return differenceObject.difference()
}

private abstract class DifferenceCalculator() {
    protected abstract val oldNameResolver: NameResolver
    protected abstract val newNameResolver: NameResolver

    protected val compareObject by lazy { ProtoCompareGenerated(oldNameResolver, newNameResolver) }

    abstract fun difference(): DifferenceKind

    protected fun membersOrNone(names: Collection<String>): DifferenceKind = if (names.isEmpty()) DifferenceKind.NONE else DifferenceKind.MEMBERS(names)

    protected fun calcDifferenceForMembers(
            oldList: List<ProtoBuf.Callable>,
            newList: List<ProtoBuf.Callable>
    ): Collection<String> {
        val result = hashSetOf<String>()

        val oldMap = oldList.groupBy { it.hashCode({ compareObject.oldGetIndexOfString(it) }, { compareObject.oldGetIndexOfClassId(it) } )}
        val newMap = newList.groupBy { it.hashCode({ compareObject.newGetIndexOfString(it) }, { compareObject.newGetIndexOfClassId(it) } )}

        fun List<ProtoBuf.Callable>.names(nameResolver: NameResolver): List<String> =
                map { nameResolver.getString(it.name) }

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
            oldList: List<ProtoBuf.Callable>,
            newList: List<ProtoBuf.Callable>
    ): Collection<String> {
        val result = hashSetOf<String>()
        val newSet = HashSet(newList)

        oldList.forEach { oldMember ->
            val newMember = newSet.firstOrNull { compareObject.checkEquals(oldMember, it) }
            if (newMember != null) {
                newSet.remove(newMember)
            }
            else {
                result.add(compareObject.oldNameResolver.getString(oldMember.name))
            }
        }

        newSet.forEach { newMember ->
            result.add(compareObject.newNameResolver.getString(newMember.name))
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
}

private class DifferenceCalculatorForClass(oldData: ProtoMapValue, newData: ProtoMapValue) : DifferenceCalculator() {
    companion object {
        private val CONSTRUCTOR = "<init>"

        private val CLASS_SIGNATURE_ENUMS = EnumSet.of(
                ProtoCompareGenerated.ProtoBufClassKind.FLAGS,
                ProtoCompareGenerated.ProtoBufClassKind.FQ_NAME,
                ProtoCompareGenerated.ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoCompareGenerated.ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoCompareGenerated.ProtoBufClassKind.CLASS_ANNOTATION_LIST
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

        for (kind in diff) {
            when (kind!!) {
                ProtoCompareGenerated.ProtoBufClassKind.COMPANION_OBJECT_NAME -> {
                    if (oldProto.hasCompanionObjectName()) oldProto.companionObjectName.oldToNames()
                    if (newProto.hasCompanionObjectName()) newProto.companionObjectName.newToNames()
                }
                ProtoCompareGenerated.ProtoBufClassKind.NESTED_CLASS_NAME_LIST ->
                    names.addAll(calcDifferenceForNames(oldProto.nestedClassNameList, newProto.nestedClassNameList))
                ProtoCompareGenerated.ProtoBufClassKind.MEMBER_LIST -> {
                    val oldMembers = oldProto.memberList.filter { !it.isPrivate }
                    val newMembers = newProto.memberList.filter { !it.isPrivate }
                    names.addAll(calcDifferenceForMembers(oldMembers, newMembers))
                }
                ProtoCompareGenerated.ProtoBufClassKind.ENUM_ENTRY_LIST ->
                    names.addAll(calcDifferenceForNames(oldProto.enumEntryList, newProto.enumEntryList))
                ProtoCompareGenerated.ProtoBufClassKind.PRIMARY_CONSTRUCTOR ->
                    if (areNonPrivatePrimaryConstructorsDifferent()) {
                        names.add(CONSTRUCTOR)
                    }
                ProtoCompareGenerated.ProtoBufClassKind.SECONDARY_CONSTRUCTOR_LIST ->
                    if (areNonPrivateSecondaryConstructorsDifferent()) {
                        names.add(CONSTRUCTOR)
                    }
                ProtoCompareGenerated.ProtoBufClassKind.FLAGS,
                ProtoCompareGenerated.ProtoBufClassKind.FQ_NAME,
                ProtoCompareGenerated.ProtoBufClassKind.TYPE_PARAMETER_LIST,
                ProtoCompareGenerated.ProtoBufClassKind.SUPERTYPE_LIST,
                ProtoCompareGenerated.ProtoBufClassKind.CLASS_ANNOTATION_LIST ->
                    throw IllegalArgumentException("Unexpected kind: $kind")
                else ->
                    throw IllegalArgumentException("Unsupported kind: $kind")
            }
        }
        return names
    }

    private fun areNonPrivatePrimaryConstructorsDifferent(): Boolean {
        val oldPrimaryConstructor = oldProto.getNonPrivatePrimaryConstructor
        val newPrimaryConstructor = newProto.getNonPrivatePrimaryConstructor
        if (oldPrimaryConstructor == null && newPrimaryConstructor == null) return false

        if (oldPrimaryConstructor == null || newPrimaryConstructor == null) return true

        return !compareObject.checkEquals(oldPrimaryConstructor, newPrimaryConstructor)
    }

    private fun areNonPrivateSecondaryConstructorsDifferent(): Boolean {
        val oldSecondaryConstructors = oldProto.secondaryConstructorList.filter { !it.isPrivate }
        val newSecondaryConstructors = newProto.secondaryConstructorList.filter { !it.isPrivate }
        return (oldSecondaryConstructors.size() != newSecondaryConstructors.size() ||
            oldSecondaryConstructors.indices.any { !compareObject.checkEquals(oldSecondaryConstructors[it], newSecondaryConstructors[it]) })
    }

    private val ProtoBuf.Class.getNonPrivatePrimaryConstructor: ProtoBuf.Class.PrimaryConstructor?
        get() {
            if (!hasPrimaryConstructor()) return null

            return if (primaryConstructor?.data?.isPrivate ?: false) null else primaryConstructor
        }

    private val ProtoBuf.Callable.isPrivate: Boolean
        get() = Visibilities.isPrivate(Deserialization.visibility(Flags.VISIBILITY.get(flags)))
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

        for (kind in diff) {
            when (kind!!) {
                ProtoCompareGenerated.ProtoBufPackageKind.MEMBER_LIST ->
                    names.addAll(calcDifferenceForMembers(oldProto.memberList, newProto.memberList))
                else ->
                    throw IllegalArgumentException("Unsupported kind: $kind")
            }
        }
        return names
    }
}
