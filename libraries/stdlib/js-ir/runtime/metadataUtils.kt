/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

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

// There was a problem with per-module compilation (KT-55758) when the top-level state (iid) was reinitialized during stdlib module initialization
// As a result we miss already incremented iid and had the same iids in two different modules
// So, to keep the state consistent it was moved into the variable without initializer and function
@Suppress("MUST_BE_INITIALIZED")
private var iid: dynamic

private fun generateInterfaceId(): Int {
    if (iid === VOID) {
        iid = 0
    }
    iid = iid.unsafeCast<Int>() + 1
    return iid.unsafeCast<Int>()
}


internal fun interfaceMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata("interface", name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity, generateInterfaceId())
}

internal fun objectMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata("object", name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity, null)
}

internal fun classMeta(
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?
): Metadata {
    return createMetadata("class", name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity, null)
}

// Seems like we need to disable this check if variables are used inside js annotation
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
private fun createMetadata(
    kind: String,
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
    iid: Int?
): Metadata {
    val undef = VOID
    return js("""({
    kind: kind,
    simpleName: name,
    associatedObjectKey: associatedObjectKey,
    associatedObjects: associatedObjects,
    suspendArity: suspendArity,
    ${'$'}kClass$: undef,
    defaultConstructor: defaultConstructor,
    iid: iid
})""")
}

internal external interface Metadata {
    val kind: String
    // This field gives fast access to the prototype of metadata owner (Object.getPrototypeOf())
    // Can be pre-initialized or lazy initialized and then should be immutable
    val simpleName: String?
    val associatedObjectKey: Number?
    val associatedObjects: dynamic
    val suspendArity: Array<Int>?
    val iid: Int?

    var `$kClass$`: dynamic
    val defaultConstructor: dynamic

    var errorInfo: Int? // Bits set for overridden properties: "message" => 0x1, "cause" => 0x2
}