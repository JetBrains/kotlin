/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class KonanClassDataFinder(
    private val fragment: KonanProtoBuf.LinkDataPackageFragment,
    private val nameResolver: NameResolver
) : ClassDataFinder {

    override fun findClassData(classId: ClassId): ClassData? {
        val proto = fragment.classes
        val nameList = proto.classNameList

        val index = nameList.indexOfFirst { nameResolver.getClassId(it) == classId }
        if (index == -1)
            error("Could not find serialized class $classId")

        val foundClass = proto.getClasses(index) ?: error("Could not find data for serialized class $classId")

        /* TODO: binary version supposed to be read from protobuf. */
        return ClassData(nameResolver, foundClass, KonanMetadataVersion.INSTANCE, SourceElement.NO_SOURCE)
    }
}
