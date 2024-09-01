package kdump

import base.Endianness
import text.*

fun Pretty.literal(string: String) = item {
    append('"')
    appendNonISOControl { append(string) }
    append('"')
}

fun Pretty.id(id: Id) = item {
    append("0x${id.long.toULong().toString(16)}")
}

fun Pretty.item(memoryDump: MemoryDump) {
    struct("dump") {
        header(memoryDump)
        item(memoryDump.endianness)
        item(memoryDump.idSize)
        struct("items") {
            memoryDump.items.forEach { item(it) }
        }
    }
}

fun Pretty.header(memoryDump: MemoryDump) {
    field("header") {
        literal(memoryDump.headerString)
    }
}

fun Pretty.item(endianness: Endianness) {
    field("endianness") {
        name(endianness)
    }
}

fun Pretty.item(idSize: IdSize) {
    field("id size") {
        decimal(idSize.byteCount)
    }
}

fun Pretty.item(item: Item) {
    when (item) {
        is Type ->
            struct("type") {
                field("id") { id(item.id) }
                field("super type id") { id(item.superTypeId) }
                field("package name") { literal(item.packageName) }
                field("relative name") { literal(item.relativeName) }
                field("body") { item(item.body) }
            }

        is ObjectItem ->
            struct("object") {
                field("id") { id(item.id) }
                field("type id") { id(item.typeId) }
                struct("bytes") { binary(item.byteArray) }
            }

        is ArrayItem ->
            struct("array") {
                field("id") { id(item.id) }
                field("type id") { id(item.typeId) }
                field("count") { decimal(item.count) }
                struct("bytes") { binary(item.byteArray) }
            }

        is ExtraObject ->
            struct("extra object") {
                field("id") { id(item.id) }
                field("base object id") { id(item.baseObjectId) }
                field("associated object id") { id(item.associatedObjectId) }
            }

        is GlobalRoot ->
            struct("global root") {
                field("source") { name(item.source) }
                field("object id") { id(item.objectId) }
            }

        is Thread ->
            struct("thread") {
                field("id") { id(item.id) }
            }

        is ThreadRoot ->
            struct("thread root") {
                field("thread id") { id(item.threadId) }
                field("source") { name(item.source) }
                field("object id") { id(item.objectId) }
            }
    }
}

fun Pretty.item(body: Type.Body) {
    when (body) {
        is Type.Body.Array -> struct("array") {
            field("element size") { decimal(body.elementSize) }
            field("is object array") { boolean(body.isObjectArray) }
            body.debug?.let { item(it) }
        }

        is Type.Body.Object -> struct("object") {
            field("instance size") { decimal(body.instanceSize) }
            struct("object offsets") { body.objectOffsets.forEach { decimal(it) } }
            body.debug?.let { item(it) }
        }
    }
}

fun Pretty.item(debug: Type.Body.Object.Debug) {
    struct("extra") {
        struct("fields") {
            debug.fields.forEach { item(it) }
        }
    }
}

fun Pretty.item(debug: Type.Body.Array.Debug) {
    struct("extra") {
        field("element type") { name(debug.elementType) }
    }
}

fun Pretty.item(field: Field) {
    struct("field") {
        field("offset") { decimal(field.offset) }
        field("type") { name(field.type) }
        field("name") { literal(field.name) }
    }
}
