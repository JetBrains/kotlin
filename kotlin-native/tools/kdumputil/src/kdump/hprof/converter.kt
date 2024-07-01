package kdump.hprof

import base.*
import hprof.SerialNumber
import hprof.write
import kdump.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import hprof.ClassDump as HProfClassDump
import hprof.HeapDump as HProfHeapDump
import hprof.Id as HProfId
import hprof.IdSize as HProfIdSize
import hprof.InstanceDump as HProfInstanceDump
import hprof.InstanceField as HProfInstanceField
import hprof.LoadClass as HProfLoadClass
import hprof.ObjectArrayDump as HProfObjectArrayDump
import hprof.PrimitiveArrayDump as HProfPrimitiveArrayDump
import hprof.Profile as HProfProfile
import hprof.RootJavaFrame as HProfRootJavaFrame
import hprof.RootJniGlobal as HProfRootJniGlobal
import hprof.RootStickyClass as HProfRootStickyClass
import hprof.RootThreadObject as HProfRootThreadObject
import hprof.StackFrame as HProfStackFrame
import hprof.StackTrace as HProfStackTrace
import hprof.StartThread as HProfStartThread
import hprof.StringConstant as HProfStringConstant
import hprof.Type as HProfType
import hprof.Writer as HProfWriter
import hprof.size as hprofSize

const val SYNTHESIZE_JAVA_LANG_STRINGS = false

private val hprofIdSize = HProfIdSize.LONG

private val HProfType.size: Int get() = hprofSize(hprofIdSize)

fun MemoryDump.newConverter(): Converter =
        Converter(
                endianness = endianness,
                idSize = idSize,
                idToItemMap = idToItemMap,
        )

fun MemoryDump.toHProfProfile(): HProfProfile =
        newConverter().also { it.add(this) }.buildProfile()

class Converter(
        private val endianness: Endianness,
        private val idSize: IdSize,
        private val idToItemMap: Map<Id, Item>,
) {
    private val profileTime: Long = System.currentTimeMillis()
    private val idToHProfIdMutableMap: MutableMap<Id, HProfId> = mutableMapOf()
    private val stringToIdMutableMap: MutableMap<String, HProfId> = mutableMapOf()
    private val hprofIdToStringMutableMap: MutableMap<HProfId, String> = mutableMapOf()
    private val hprofProfileRecords: MutableList<HProfProfile.Record> = mutableListOf()
    private val hprofHeapDumpRecords: MutableList<HProfHeapDump.Record> = mutableListOf()
    private val hprofExtraClassObjectIds: MutableMap<String, HProfId> = mutableMapOf()
    private val kotlinToJavaHprofIdMutableMap: MutableMap<HProfId, HProfId> = mutableMapOf()
    private val typeIdToSyntheticFieldsMap: MutableMap<Id, List<Field>> = mutableMapOf()
    private var lastClassSerialNumber: SerialNumber = SerialNumber(0)
    private val threadIdToSerialNumberMap: MutableMap<Id, SerialNumber> = mutableMapOf()
    private var nextFreeHProfObjectAddress: Long = 0x20000000L
    private val syntheticClassNames: MutableSet<String> = mutableSetOf()

    fun OutputStream.hprofWriter(): HProfWriter =
            HProfWriter(this, hprofIdSize)

    fun hprofByteArray(fn: HProfWriter.() -> Unit): ByteArray =
            ByteArrayOutputStream().apply { hprofWriter().fn() }.toByteArray()

    fun size(type: RuntimeType): Int = type.size(idSize)

    fun hprofInstanceSize(typeId: Id, objectTypeBody: Type.Body.Object): Int =
            fields(typeId, objectTypeBody).fold(0) { acc, field ->
                acc + field.type.hprofTypes.sumOf { it.size }
            }

    fun getId(byteArray: ByteArray, offset: Int): Id =
            when (idSize) {
                IdSize.BITS_8 -> id(byteArray.getByte(offset))
                IdSize.BITS_16 -> id(byteArray.getShort(offset, endianness))
                IdSize.BITS_32 -> id(byteArray.getInt(offset, endianness))
                IdSize.BITS_64 -> id(byteArray.getLong(offset, endianness))
            }

    fun HProfWriter.writeObjectValue(byteArray: ByteArray, offset: Int) {
        write(hprofObjectReferenceId(getId(byteArray, offset)))
    }

    fun HProfWriter.writeValue(
            byteArray: ByteArray,
            offset: Int,
            runtimeType: RuntimeType,
    ) {
        when (runtimeType) {
            RuntimeType.OBJECT ->
                writeObjectValue(byteArray, offset)

            RuntimeType.INT_8 ->
                write(byteArray[offset])

            RuntimeType.INT_16 ->
                write(byteArray.getShort(offset, endianness))

            RuntimeType.INT_32 ->
                write(byteArray.getInt(offset, endianness))

            RuntimeType.INT_64 ->
                write(byteArray.getLong(offset, endianness))

            RuntimeType.FLOAT_32 ->
                write(byteArray.getFloat(offset, endianness))

            RuntimeType.FLOAT_64 ->
                write(byteArray.getDouble(offset, endianness))

            RuntimeType.NATIVE_PTR ->
                write(byteArray.getLong(offset, endianness))

            RuntimeType.BOOLEAN ->
                write(byteArray[offset])

            RuntimeType.VECTOR_128 ->
                when (endianness) {
                    Endianness.BIG -> {
                        write(byteArray.getInt(offset + 0, endianness))
                        write(byteArray.getInt(offset + 4, endianness))
                        write(byteArray.getInt(offset + 8, endianness))
                        write(byteArray.getInt(offset + 12, endianness))
                    }

                    Endianness.LITTLE -> {
                        write(byteArray.getInt(offset + 12, endianness))
                        write(byteArray.getInt(offset + 8, endianness))
                        write(byteArray.getInt(offset + 4, endianness))
                        write(byteArray.getInt(offset + 0, endianness))
                    }
                }
        }
    }

    fun HProfWriter.writeValue(byteArray: ByteArray, field: Field) =
            writeValue(byteArray, field.offset, field.type)

    fun HProfWriter.writeValues(byteArray: ByteArray, fields: List<Field>) =
            fields.forEach { field ->
                writeValue(byteArray, field)
            }

    fun HProfWriter.writeFieldValues(byteArray: ByteArray, type: Type) {
        writeValues(byteArray, directFields(type))

        superType(type)?.let { superType ->
            writeFieldValues(byteArray, superType)
        }
    }

    fun HProfWriter.writeArray(
            byteArray: ByteArray,
            offset: Int,
            count: Int,
            elementType: RuntimeType,
    ) {
        size(elementType).let { elementSize ->
            repeat(count) { index ->
                writeValue(byteArray, offset + index * elementSize, elementType)
            }
        }
    }

    fun hprofId(string: String): HProfId =
            stringToIdMutableMap.getOrPut(string) {
                stringToIdMutableMap.size
                        .toLong()
                        .inc()
                        .times(8)
                        .or(0x100000000L)
                        .let(::HProfId)
                        .also { id ->
                            hprofIdToStringMutableMap[id] = string
                            hprofProfileRecords.add(HProfStringConstant(id, string))
                        }
            }

    fun hprofObjectId(id: Id): HProfId =
            when (id.long) {
                0L -> HProfId.NULL
                else -> idToHProfIdMutableMap.getOrPut(id) {
                    newHProfObjectId(item(id).size(idSize)!!)
                }
            }

    fun extraClassObjectId(className: String): HProfId =
            hprofExtraClassObjectIds.getOrPut(className) {
                newHProfObjectId(64)
            }

    fun hprofObjectReferenceId(id: Id): HProfId =
            hprofObjectId(id).let { hprofId ->
                kotlinToJavaHprofIdMutableMap.getOrDefault(hprofId, hprofId)
            }

    fun newHProfObjectId(size: Int): HProfId =
            nextFreeHProfObjectAddress
                    .also { nextFreeHProfObjectAddress = it.plus(size.align(8)) }
                    .let { HProfId(it) }

    fun hprofClassObjectId(type: Type): HProfId =
            type.body.let { typeBody ->
                when (typeBody) {
                    is Type.Body.Array ->
                        extraClassObjectId(
                                when (typeBody.debug?.elementType) {
                                    RuntimeType.OBJECT -> ClassName.Array.OBJECT
                                    RuntimeType.INT_8 -> ClassName.Array.BYTE
                                    RuntimeType.INT_16 -> ClassName.Array.SHORT
                                    RuntimeType.INT_32 -> ClassName.Array.INT
                                    RuntimeType.INT_64 -> ClassName.Array.LONG
                                    RuntimeType.FLOAT_32 -> ClassName.Array.FLOAT
                                    RuntimeType.FLOAT_64 -> ClassName.Array.DOUBLE
                                    RuntimeType.NATIVE_PTR -> ClassName.Array.LONG
                                    RuntimeType.BOOLEAN -> ClassName.Array.BOOLEAN
                                    RuntimeType.VECTOR_128 -> ClassName.Array.INT
                                    else -> when (typeBody.elementSize) {
                                        1 -> ClassName.Array.BYTE
                                        2 -> ClassName.Array.SHORT
                                        4 -> ClassName.Array.INT
                                        8 -> ClassName.Array.LONG
                                        else -> ClassName.Array.BYTE // Map everything else to byte array
                                    }
                                }
                        )

                    is Type.Body.Object ->
                        when {
                            else -> hprofObjectId(type.id)
                        }
                }
            }

    fun hprofSuperClassObjectId(type: Type): HProfId =
            superType(type)
                    ?.let { hprofClassObjectId(it) }
                    ?: extraClassObjectId(ClassName.OBJECT)

    fun nextClassSerialNumber(): SerialNumber =
            lastClassSerialNumber.int.inc().let(::SerialNumber).also { lastClassSerialNumber = it }

    fun threadSerialNumber(threadId: Id): SerialNumber =
            threadIdToSerialNumberMap.getOrPut(threadId) {
                threadIdToSerialNumberMap.size.inc().let(::SerialNumber)
            }

    fun hprofStringConstants(): List<HProfStringConstant> =
            hprofIdToStringMutableMap.map { (id, string) -> HProfStringConstant(id, string) }

    fun type(id: Id): Type = item(id) as Type

    fun item(id: Id): Item =
            idToItemMap[id] ?: throw IllegalArgumentException("No item for id: $id")

    fun superType(type: Type): Type? =
            type.superTypeId.takeIf { it.long != 0L }?.let { type(it) }

    fun syntheticFields(typeId: Id, objectTypeBody: Type.Body.Object): List<Field> =
            typeIdToSyntheticFieldsMap.getOrPut(typeId) { objectTypeBody.buildSyntheticFields(idSize) }

    fun fields(typeId: Id, objectTypeBody: Type.Body.Object): List<Field> =
            objectTypeBody.debug?.fields ?: syntheticFields(typeId, objectTypeBody)

    fun fields(type: Type): List<Field> =
            fields(type.id, type.body as Type.Body.Object)

    fun buildProfile(): HProfProfile =
            HProfProfile(
                    idSize = hprofIdSize,
                    time = profileTime,
                    records = buildList {
                        addAll(hprofProfileRecords)
                        add(HProfHeapDump(hprofHeapDumpRecords))
                    })

    fun add(memoryDump: MemoryDump) {
        syntheticClassNames.add(ClassName.CLASS)

        addSyntheticClass(ClassName.OBJECT)

        addSyntheticClass(ClassName.Array.BOOLEAN, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.CHAR, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.BYTE, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.SHORT, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.INT, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.LONG, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.FLOAT, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.DOUBLE, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.Array.OBJECT, extraClassObjectId(ClassName.OBJECT))

        addSyntheticClass(ClassName.CLASS, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.CLASS_LOADER, extraClassObjectId(ClassName.OBJECT))
        addSyntheticClass(ClassName.THREAD, extraClassObjectId(ClassName.OBJECT))

        addSyntheticClass(
                ClassName.STRING,
                extraClassObjectId(ClassName.OBJECT),
                listOf(
                        HProfInstanceField(
                                nameStringId = hprofId("value"),
                                type = HProfType.OBJECT
                        )
                )
        )

        addSyntheticClass(
                ClassName.EXTRA_OBJECT,
                extraClassObjectId(ClassName.OBJECT),
                listOf(
                        HProfInstanceField(
                                nameStringId = hprofId("baseObject"),
                                type = HProfType.OBJECT
                        ),
                        HProfInstanceField(
                                nameStringId = hprofId("associatedObject"),
                                type = HProfType.LONG
                        )
                )
        )

        if (SYNTHESIZE_JAVA_LANG_STRINGS) {
            addJavaLangStringIds(memoryDump)
        }

        memoryDump.items.forEach { add(it) }
    }

    fun addJavaLangStringIds(memoryDump: MemoryDump) {
        memoryDump.items.forEach { item ->
            if (item is ArrayItem) {
                val type = type(item.typeId)
                if (type.isKotlinString) {
                    val hprofObjectId = hprofObjectId(item.id)
                    val hprofStringObjectId = newHProfObjectId(idSize.byteCount * 2)
                    kotlinToJavaHprofIdMutableMap[hprofObjectId] = hprofStringObjectId
                }
            }
        }
    }

    fun addJavaLangStringRecords(arrayItem: ArrayItem, hprofJavaLangStringId: HProfId) {
        hprofHeapDumpRecords.add(
                HProfInstanceDump(
                        objectId = hprofJavaLangStringId,
                        classObjectId = extraClassObjectId(ClassName.STRING),
                        byteArray = hprofByteArray {
                            write(hprofObjectId(arrayItem.id))
                        }))
    }

    fun addSyntheticClass(
            className: String,
            superClassObjectId: HProfId = HProfId.NULL,
            instanceFields: List<HProfInstanceField> = listOf(),
    ) {
        val classObjectId = extraClassObjectId(className)
        val instanceSize = instanceFields.map { it.type.size }.sum()

        hprofProfileRecords.add(
                HProfLoadClass(
                        classSerialNumber = nextClassSerialNumber(),
                        classObjectId = classObjectId,
                        classNameStringId = hprofId(className)
                )
        )

        hprofHeapDumpRecords.add(
                HProfClassDump(
                        classObjectId = classObjectId,
                        superClassObjectId = superClassObjectId,
                        instanceSize = instanceSize,
                        instanceFields = instanceFields
                )
        )
    }

    fun add(item: Item) {
        when (item) {
            is Type -> addItem(item)
            is ObjectItem -> addItem(item)
            is ArrayItem -> addItem(item)
            is ExtraObject -> addItem(item)
            is Thread -> addItem(item)
            is GlobalRoot -> addItem(item)
            is ThreadRoot -> addItem(item)
        }
    }

    fun addItem(extraObject: ExtraObject) {
        hprofHeapDumpRecords.add(
                HProfInstanceDump(
                        objectId = hprofObjectId(extraObject.id),
                        classObjectId = extraClassObjectId(ClassName.EXTRA_OBJECT),
                        byteArray = hprofByteArray {
                            write(hprofObjectReferenceId(extraObject.baseObjectId))
                            write(HProfId(extraObject.associatedObjectId.long))
                        }))
    }

    fun addItem(thread: Thread) {
        val threadObjectId = hprofObjectId(thread.id)
        val threadSerialNumber = threadSerialNumber(thread.id)
        val stackTraceSerialNumber = threadSerialNumber // same as thread serial number for now

        hprofProfileRecords.add(
                HProfStartThread(
                        threadSerialNumber = threadSerialNumber,
                        threadObjectId = threadObjectId,
                        threadNameStringId = hprofId("thread-$threadSerialNumber"),
                )
        )

        val stackFrameId = newHProfObjectId(32) // Some approximation
        hprofProfileRecords.add(
                HProfStackFrame(
                        stackFrameId = stackFrameId,
                        methodNameStringId = hprofId("start"),
                        methodSignatureStringId = hprofId("()V"),
                )
        )

        hprofProfileRecords.add(
                HProfStackTrace(
                        serialNumber = stackTraceSerialNumber,
                        threadSerialNumber = threadSerialNumber,
                        stackFrameIds = listOf(stackFrameId)
                )
        )

        hprofHeapDumpRecords.add(
                HProfInstanceDump(
                        objectId = threadObjectId,
                        classObjectId = extraClassObjectId(ClassName.THREAD)
                )
        )

        hprofHeapDumpRecords.add(
                HProfRootThreadObject(
                        threadObjectId = threadObjectId,
                        threadSerialNumber = threadSerialNumber,
                        stackTraceSerialNumber = stackTraceSerialNumber
                )
        )
    }

    fun addItem(globalRoot: GlobalRoot) {
        hprofHeapDumpRecords.add(
                HProfRootJniGlobal(
                        objectId = hprofObjectReferenceId(globalRoot.objectId),
                        refId = hprofObjectReferenceId(globalRoot.objectId)
                )
        )
    }

    fun addItem(threadRoot: ThreadRoot) {
        hprofHeapDumpRecords.add(
                HProfRootJavaFrame(
                        objectId = hprofObjectReferenceId(threadRoot.objectId),
                        threadSerialNumber = threadSerialNumber(threadRoot.threadId)
                )
        )
    }

    fun addItem(type: Type) {
        when (type.body) {
            is Type.Body.Array -> {
                // All array types are synthesized.
            }

            is Type.Body.Object -> {
                hprofProfileRecords.add(hprofLoadClass(type))

                hprofHeapDumpRecords.add(
                        HProfRootStickyClass(
                                objectId = hprofClassObjectId(type)
                        )
                )

                hprofHeapDumpRecords.add(hprofClassDump(type))
            }
        }
    }

    fun hprofLoadClass(type: Type): HProfLoadClass {
        val className = type.hprofClassName.let {
            it.takeIf { !syntheticClassNames.contains(it) } ?: "$it\$Kotlin"
        }

        return HProfLoadClass(
                classSerialNumber = nextClassSerialNumber(),
                classObjectId = hprofClassObjectId(type),
                classNameStringId = hprofId(className)
        )
    }

    fun hprofClassDump(type: Type): HProfClassDump =
            type.body.let { body ->
                when (body) {
                    is Type.Body.Object ->
                        HProfClassDump(
                                classObjectId = hprofClassObjectId(type),
                                superClassObjectId = hprofSuperClassObjectId(type),
                                instanceSize = hprofInstanceSize(type.id, body),
                                instanceFields = hprofInstanceFields(type)
                        )

                    is Type.Body.Array ->
                        throw IllegalStateException("Array classes are synthesized.")
                }
            }

    fun hprofInstanceFields(type: Type): List<HProfInstanceField> =
            hprofInstanceFields(directFields(type))

    fun hprofInstanceFields(fields: List<Field>): List<HProfInstanceField> =
            fields.flatMap { hprofInstanceFields(it) }

    fun hprofInstanceFields(field: Field): List<HProfInstanceField> = run {
        val hprofTypes = field.type.hprofTypes
        val isVector = hprofTypes.size > 1
        hprofTypes.mapIndexed { index, hprofType ->
            val name = field.name.runIf(isVector) { plus("_$index") }
            HProfInstanceField(
                    nameStringId = hprofId(name),
                    type = hprofType,
            )
        }
    }

    fun directFields(type: Type): List<Field> =
            fields(type).drop(superType(type)?.let(::fields)?.size ?: 0)

    fun addItem(objectItem: ObjectItem) {
        hprofHeapDumpRecords.add(hprofDumpRecord(objectItem))
    }

    fun addItem(arrayItem: ArrayItem) {
        if (SYNTHESIZE_JAVA_LANG_STRINGS) {
            val hprofObjectId = hprofObjectId(arrayItem.id)
            val hprofStringId = kotlinToJavaHprofIdMutableMap[hprofObjectId]
            if (hprofStringId != null) {
                addJavaLangStringRecords(arrayItem, hprofStringId)
            }
        }
        hprofHeapDumpRecords.add(hprofDumpRecord(arrayItem))
    }

    fun hprofDumpRecord(objectItem: ObjectItem): HProfHeapDump.Record {
        val objectId = hprofObjectId(objectItem.id)
        val type = type(objectItem.typeId)
        val byteArray = objectItem.byteArray
        return hprofInstanceDump(objectId, type, byteArray)
    }

    fun hprofDumpRecord(arrayItem: ArrayItem): HProfHeapDump.Record {
        val objectId = hprofObjectId(arrayItem.id)
        val type = type(arrayItem.typeId)
        val count = arrayItem.count
        val byteArray = arrayItem.byteArray

        val offset = 0

        return if (type.packageName != "kotlin") {
            throw IllegalArgumentException("Unknown array type package name: ${type.packageName}")
        } else when (type.relativeName) {
            "Array" ->
                hprofObjectArrayDump(objectId, hprofClassObjectId(type), byteArray, offset, count)

            else -> {
                val (runtimeElementType, hprofElementType) =
                        type.relativeName.primitiveArrayClassNameToElementTypePair()
                hprofPrimitiveArrayDump(
                        objectId,
                        hprofElementType,
                        byteArray,
                        offset,
                        count,
                        runtimeElementType
                )
            }
        }
    }

    fun hprofInstanceDump(
            hprofObjectId: HProfId,
            type: Type,
            byteArray: ByteArray,
    ): HProfInstanceDump {
        return HProfInstanceDump(
                objectId = hprofObjectId,
                classObjectId = hprofClassObjectId(type),
                byteArray = hprofByteArray {
                    writeFieldValues(byteArray, type)
                })
    }

    fun hprofPrimitiveArrayDump(
            hprofObjectId: HProfId,
            hprofElementType: HProfType,
            byteArray: ByteArray,
            offset: Int,
            count: Int,
            elementRuntimeType: RuntimeType,
    ): HProfPrimitiveArrayDump {
        return HProfPrimitiveArrayDump(
                arrayObjectId = hprofObjectId,
                numberOfElements = count,
                arrayElementType = hprofElementType,
                byteArray = hprofByteArray {
                    writeArray(byteArray, offset, count, elementRuntimeType)
                })
    }

    fun hprofObjectArrayDump(
            hprofObjectId: HProfId,
            hprofArrayClassObjectId: HProfId,
            byteArray: ByteArray,
            offset: Int,
            count: Int,
    ): HProfObjectArrayDump {
        return HProfObjectArrayDump(
                arrayObjectId = hprofObjectId,
                numberOfElements = count,
                arrayClassObjectId = hprofArrayClassObjectId,
                byteArray = hprofByteArray {
                    writeArray(byteArray, offset, count, RuntimeType.OBJECT)
                })
    }
}
