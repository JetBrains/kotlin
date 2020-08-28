package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifierId
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId

@Suppress("MemberVisibilityCanBePrivate")
object CirClassifierIdFactory {
    private val interner = Interner<CirClassifierId>()

    fun create(source: ClassifierDescriptor): CirClassifierId {
        return when (source) {
            is ClassDescriptor -> createForClass(source.internedClassId)
            is TypeAliasDescriptor -> createForTypeAlias(source.internedClassId)
            is TypeParameterDescriptor -> createForTypeParameter(source.typeParameterIndex)
            else -> error("Unexpected classifier descriptor type: ${source::class.java}, $this")
        }
    }

    fun createForClass(classId: ClassId): CirClassifierId = interner.intern(CirClassifierId.Class(classId))
    fun createForTypeAlias(classId: ClassId): CirClassifierId = interner.intern(CirClassifierId.TypeAlias(classId))
    fun createForTypeParameter(index: Int): CirClassifierId = interner.intern(CirClassifierId.TypeParameter(index))
}

private inline val TypeParameterDescriptor.typeParameterIndex: Int
    get() {
        var index = index
        var parent = containingDeclaration

        while ((parent as? ClassifierDescriptorWithTypeParameters)?.isInner != false) {
            parent = parent.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: break
            index += parent.declaredTypeParameters.size
        }

        return index
    }
