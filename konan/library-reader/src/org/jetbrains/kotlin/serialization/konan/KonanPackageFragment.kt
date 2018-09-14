/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.StorageManager

class KonanPackageFragment(
    fqName: FqName,
    private val library: KonanLibrary,
    private val packageAccessedHandler: PackageAccessedHandler?,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    val partName: String
) : DeserializedPackageFragment(fqName, storageManager, module) {

    lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    // The proto field is lazy so that we can load only needed
    // packages from the library.
    private val protoForNames: KonanProtoBuf.LinkDataPackageFragment
        get() =
            protoForNamesStore ?: library.packageMetadata(fqName.asString(), partName).also { protoForNamesStore = it }

    private var protoForNamesStore: KonanProtoBuf.LinkDataPackageFragment? = null

    val proto: KonanProtoBuf.LinkDataPackageFragment
        get() = protoForNames.also { packageAccessedHandler?.markPackageAccessed(fqName) }

    private val nameResolver by lazy {
        NameResolverImpl(protoForNames.stringTable, protoForNames.nameTable)
    }

    override val classDataFinder by lazy {
        KotlinNativeProtoBasedClassDataFinder(proto, nameResolver)
    }

    private val _memberScope by lazy {
        val classNameList = protoForNames.classes.classNameList
        /* TODO: we fake proto binary versioning for now. */
        val memberScope = DeserializedPackageMemberScope(
            this,
            proto.getPackage(),
            nameResolver,
            KonanMetadataVersion.INSTANCE,
            /* containerSource = */ null,
            components
        ) { loadClassNames(classNameList) }
        // Clear hard ref to proto, as class fully initialized
        protoForNamesStore = null
        memberScope
    }

    override fun getMemberScope(): DeserializedPackageMemberScope = _memberScope

    private val classifierNames: Set<Name> by lazy {
        val result = mutableSetOf<Name>()
        result.addAll(loadClassNames(protoForNames.classes.classNameList))
        protoForNames.getPackage().typeAliasList.mapTo(result) { nameResolver.getName(it.name) }
        result
    }

    fun hasTopLevelClassifier(name: Name): Boolean = name in classifierNames

    private fun loadClassNames(classNameList: List<Int>): Collection<Name> {

        val names = classNameList.mapNotNull {
            val classId = nameResolver.getClassId(it)
            val shortName = classId.shortClassName
            if (!classId.isNestedClass) shortName else null
        }

        return names
    }
}
