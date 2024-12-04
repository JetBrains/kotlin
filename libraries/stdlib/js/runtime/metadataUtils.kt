/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

// There was a problem with per-module compilation (KT-55758) when the top-level state (globalInterfaceId) was reinitialized during stdlib module initialization
// As a result we miss already incremented globalInterfaceId and had the same interfaceIds in two different modules
// So, to keep the state consistent it was moved into the variable without initializer and function
@Suppress("MUST_BE_INITIALIZED")
private var globalInterfaceId: dynamic

private fun generateInterfaceId(): Int {
    if (globalInterfaceId === VOID) {
        globalInterfaceId = 0
    }
    globalInterfaceId = globalInterfaceId.unsafeCast<Int>() + 1
    return globalInterfaceId.unsafeCast<Int>()
}

internal const val METADATA_KIND_INTERFACE = "interface"
internal const val METADATA_KIND_OBJECT = "object"
internal const val METADATA_KIND_CLASS = "class"

internal fun initMetadataFor(
    kind: String,
    ctor: Ctor,
    name: String?,
    defaultConstructor: dynamic,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    suspendArity: Array<Int>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic
) {
    if (parent != null) {
        js("""
          ctor.prototype = Object.create(parent.prototype)
          ctor.prototype.constructor = ctor;
        """)
    }

    val metadata = createMetadata(kind, name, defaultConstructor, associatedObjectKey, associatedObjects, suspendArity)
    ctor.`$metadata$` = metadata

    if (interfaces != null) {
        val receiver = if (metadata.iid != VOID) ctor else ctor.prototype
        receiver.`$imask$` = implement(interfaces)
    }
}

internal fun initMetadataForClass(
    ctor: Ctor,
    name: String?,
    defaultConstructor: dynamic,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    suspendArity: Array<Int>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic
) {
    val kind = METADATA_KIND_CLASS
    initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects)
}

internal fun initMetadataForObject(
    ctor: Ctor,
    name: String?,
    defaultConstructor: dynamic,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    suspendArity: Array<Int>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic
) {
    val kind = METADATA_KIND_OBJECT
    initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects)
}

internal fun initMetadataForInterface(
    ctor: Ctor,
    name: String?,
    defaultConstructor: dynamic,
    parent: Ctor?,
    interfaces: Array<dynamic>?,
    suspendArity: Array<Int>?,
    associatedObjectKey: Number?,
    associatedObjects: dynamic
) {
    val kind = METADATA_KIND_INTERFACE
    initMetadataFor(kind, ctor, name, defaultConstructor, parent, interfaces, suspendArity, associatedObjectKey, associatedObjects)
}

internal fun initMetadataForLambda(ctor: Ctor, parent: Ctor?, interfaces: Array<dynamic>?, suspendArity: Array<Int>?) {
    initMetadataForClass(ctor, "Lambda", VOID, parent, interfaces, suspendArity, VOID, VOID)
}

internal fun initMetadataForFunctionReference(ctor: Ctor, parent: Ctor?, interfaces: Array<dynamic>?, suspendArity: Array<Int>?) {
    initMetadataForClass(ctor, "FunctionReference", VOID, parent, interfaces, suspendArity, VOID, VOID)
}

internal fun initMetadataForCoroutine(ctor: Ctor, parent: Ctor?, interfaces: Array<dynamic>?, suspendArity: Array<Int>?) {
    initMetadataForClass(ctor, "Coroutine", VOID, parent, interfaces, suspendArity, VOID, VOID)
}

internal fun initMetadataForCompanion(ctor: Ctor, parent: Ctor?, interfaces: Array<dynamic>?, suspendArity: Array<Int>?) {
    initMetadataForObject(ctor, "Companion", VOID, parent, interfaces, suspendArity, VOID, VOID)
}

// Seems like we need to disable this check if variables are used inside js annotation
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
internal fun createMetadata(
    kind: String,
    name: String?,
    defaultConstructor: dynamic,
    associatedObjectKey: Number?,
    associatedObjects: dynamic,
    suspendArity: Array<Int>?,
): Metadata {
    val undef = VOID
    val iid = if (kind == METADATA_KIND_INTERFACE) generateInterfaceId() else VOID
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
    // TODO: This field can be used from user libs, e.g. kotlinx.serialization:
    //  https://github.com/Kotlin/kotlinx.serialization/blob/b8de86f0e351f1099d2afb03ff92e2ef6256cbc7/core/jsMain/src/kotlinx/serialization/internal/Platform.kt#L74
    //  This must be reworked
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
