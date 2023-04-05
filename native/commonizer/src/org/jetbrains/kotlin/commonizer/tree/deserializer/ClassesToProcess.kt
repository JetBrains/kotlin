/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import com.intellij.util.containers.FactoryMap
import kotlinx.metadata.Flag
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmClass
import kotlinx.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.klibEnumEntries
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.utils.NON_EXISTING_CLASSIFIER_ID

internal class ClassesToProcess {
    sealed class ClassEntry {
        abstract val classId: CirEntityId

        data class RegularClassEntry(
            override val classId: CirEntityId,
            val clazz: KmClass
        ) : ClassEntry()

        data class EnumEntry(
            override val classId: CirEntityId,
            val annotations: List<KmAnnotation>,
            val enumClassId: CirEntityId,
            val enumClass: KmClass
        ) : ClassEntry()
    }

    // key = parent class ID (or NON_EXISTING_CLASSIFIER_ID for top-level classes)
    // value = classes under this parent class (MutableList to preserve order of classes)
    private val groupedByParentClassId = FactoryMap.create<CirEntityId, MutableList<ClassEntry>> { ArrayList() }

    fun addClassesFromFragment(fragment: KmModuleFragment) {
        val klibEnumEntries = LinkedHashMap<CirEntityId, ClassEntry.EnumEntry>() // linked hash map to preserve order
        val regularClassIds = HashSet<CirEntityId>()

        fragment.classes.forEach { clazz ->
            val classId: CirEntityId = CirEntityId.create(clazz.name)
            val parentClassId: CirEntityId = classId.getParentEntityId() ?: NON_EXISTING_CLASSIFIER_ID

            if (Flag.Class.IS_ENUM_CLASS(clazz.flags)) {
                clazz.klibEnumEntries.forEach { entry ->
                    val enumEntryId = classId.createNestedEntityId(CirName.create(entry.name))
                    klibEnumEntries[enumEntryId] = ClassEntry.EnumEntry(enumEntryId, entry.annotations, classId, clazz)
                }
            }

            groupedByParentClassId.getValue(parentClassId) += ClassEntry.RegularClassEntry(classId, clazz)
            regularClassIds += classId
        }

        // add enum entries that are not stored in module as KmClass records
        klibEnumEntries.forEach { (enumEntryId, enumEntry) ->
            if (enumEntryId !in regularClassIds) {
                groupedByParentClassId.getValue(enumEntry.enumClassId) += enumEntry
            }
        }
    }

    fun classesInScope(parentClassId: CirEntityId?): List<ClassEntry> {
        return groupedByParentClassId[parentClassId ?: NON_EXISTING_CLASSIFIER_ID].orEmpty()
    }

    fun forEachClassInScope(parentClassId: CirEntityId?, block: (ClassEntry) -> Unit) {
        classesInScope(parentClassId).forEach { classEntry -> block(classEntry) }
    }
}
