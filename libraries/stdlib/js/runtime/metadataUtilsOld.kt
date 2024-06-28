/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

// TODO: Remove this file after bootstrap

internal fun setMetadataFor(
    ctor: Ctor,
    name: String?,
    metadataConstructor: (name: String?, defaultConstructor: dynamic, associatedObjectKey: Number?, associatedObjects: dynamic, suspendArity: Array<Int>?) -> Metadata,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
) {
    if (parent != null) {
        js("""
          ctor.prototype = Object.create(parent.prototype)
          ctor.prototype.constructor = ctor;
        """)
    }

    val metadata = metadataConstructor(name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity ?: js("[]"))
    ctor.`$metadata$` = metadata

    if (interfaces != null) {
        val receiver = if (metadata.iid != null) ctor else ctor.prototype
        receiver.`$imask$` = implement(interfaces)
    }
}

internal fun interfaceMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata(METADATA_KIND_INTERFACE, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity)
}

internal fun objectMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata(METADATA_KIND_OBJECT, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity)
}

internal fun classMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata(METADATA_KIND_CLASS, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity)
}
