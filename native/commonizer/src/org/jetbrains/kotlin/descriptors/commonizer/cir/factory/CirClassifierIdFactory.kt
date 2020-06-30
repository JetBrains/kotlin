package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifierId
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@Suppress("MemberVisibilityCanBePrivate")
object CirClassifierIdFactory {
    private val interner = Interner<CirClassifierId>()

    fun create(source: ClassifierDescriptor): CirClassifierId {
        return when (source) {
            is ClassDescriptor -> createForClass(source.internedClassId)
            is TypeAliasDescriptor -> createForTypeAlias(source.internedClassId)
            is TypeParameterDescriptor -> createForTypeParameter(source.name.intern())
            else -> error("Unexpected classifier descriptor type: ${source::class.java}, $this")
        }
    }

    fun createForClass(classId: ClassId): CirClassifierId = interner.intern(CirClassifierId.Class(classId))
    fun createForTypeAlias(classId: ClassId): CirClassifierId = interner.intern(CirClassifierId.TypeAlias(classId))
    fun createForTypeParameter(name: Name): CirClassifierId = interner.intern(CirClassifierId.TypeParameter(name))
}
