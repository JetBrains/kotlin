/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import java.io.BufferedReader
import java.io.Writer

internal class ObjCExportUnitEvent(val isEarly: Boolean, val classId: String)

internal sealed class ObjCExportUnitContent {
    class Stubs(val stubs: List<ObjCExportStub>) : ObjCExportUnitContent()
    class Lines(val lines: List<String>) : ObjCExportUnitContent()

    fun render(exportKDoc: Boolean, isTopLevel: Boolean): List<String> = when (this) {
        is Lines -> lines
        is Stubs -> stubs.flatMap { stub ->
            StubRenderer.render(stub, exportKDoc).flatMap { it.split('\n') } + if (isTopLevel) listOf("") else emptyList()
        }
    }
}

internal class ObjCExportClassUnit(
    val classId: String,
    val index: Int,
    val isInMainPass: Boolean,
    val isGenerated: Boolean,
    val events: List<ObjCExportUnitEvent>,
    val content: ObjCExportUnitContent,
)

internal class ObjCExportMemberSortKey(val isProperty: Boolean, val name: String, val parameterCount: Int, val signature: String) :
    Comparable<ObjCExportMemberSortKey> {
    override fun compareTo(other: ObjCExportMemberSortKey): Int = ORDER.compare(this, other)

    private companion object {
        val ORDER = compareBy<ObjCExportMemberSortKey>({ it.isProperty }, { it.name }, { it.parameterCount }, { it.signature })
    }
}

internal class ObjCExportCategoryMember(
    val classId: String,
    val className: String,
    val sortKey: ObjCExportMemberSortKey,
    val packageName: String,
    val index: Int,
    val thrownEvents: List<ObjCExportUnitEvent>,
    val events: List<ObjCExportUnitEvent>,
    val content: ObjCExportUnitContent,
)

internal class ObjCExportFileUnit(
    val fileName: String?,
    val packageName: String,
    val index: Int,
    val events: List<ObjCExportUnitEvent>,
    val content: ObjCExportUnitContent,
)

@InternalKotlinNativeApi
class ObjCExportUnits internal constructor(
    internal val classes: List<ObjCExportClassUnit>,
    internal val categoryMembers: List<ObjCExportCategoryMember>,
    internal val files: List<ObjCExportFileUnit>,
    internal val classForwardDeclarations: Set<ObjCClassForwardDeclaration>,
    internal val protocolForwardDeclarations: Set<String>,
    internal val classClaims: List<ObjCExportClassClaim> = emptyList(),
    internal val names: List<ObjCExportNameRecord> = emptyList(),
    internal val hasCollisions: Boolean = false,
    internal val typeParameterClashes: List<String> = emptyList(),
) {
    internal val events: Sequence<ObjCExportUnitEvent>
        get() = classes.asSequence().flatMap { it.events } +
                categoryMembers.asSequence().flatMap {
                    sequenceOf(ObjCExportUnitEvent(isEarly = true, it.classId)) + it.thrownEvents + it.events
                } +
                files.asSequence().flatMap { it.events }

    fun write(writer: Writer, exportKDoc: Boolean) {
        fun line(value: Any) {
            writer.write(value.toString())
            writer.write("\n")
        }

        fun events(events: List<ObjCExportUnitEvent>) {
            line(events.size)
            events.forEach { line((if (it.isEarly) "E " else "R ") + it.classId) }
        }

        fun content(content: ObjCExportUnitContent, isTopLevel: Boolean) {
            val lines = content.render(exportKDoc, isTopLevel)
            line(lines.size)
            lines.forEach(::line)
        }

        line(FORMAT)
        line(hasCollisions.bit)
        line(typeParameterClashes.size)
        typeParameterClashes.forEach(::line)
        line(classForwardDeclarations.size)
        classForwardDeclarations.forEach { declaration ->
            line((listOf(declaration.className) + declaration.typeDeclarations.map { it.encode() }).joinToString("\t"))
        }
        line(protocolForwardDeclarations.size)
        protocolForwardDeclarations.forEach(::line)
        line(classClaims.size)
        classClaims.forEach { line(listOf(it.classId, "${it.isFinal.bit}${it.isInterface.bit}", it.supertypes.sorted().joinToString(LIST)).joinToString("\t")) }
        line(names.size)
        names.forEach { line(it.encode()) }
        line(classes.size)
        classes.forEach {
            line(it.classId)
            line("${it.index} ${it.isInMainPass.bit} ${it.isGenerated.bit}")
            events(it.events)
            content(it.content, isTopLevel = true)
        }
        line(categoryMembers.size)
        categoryMembers.forEach {
            line(it.classId)
            line(it.className)
            line("${it.sortKey.isProperty.bit} ${it.sortKey.parameterCount} ${it.index}")
            line(it.sortKey.name)
            line(it.sortKey.signature)
            line(it.packageName)
            events(it.thrownEvents)
            events(it.events)
            content(it.content, isTopLevel = false)
        }
        line(files.size)
        files.forEach {
            line(if (it.fileName == null) "0" else "1")
            line(it.fileName.orEmpty())
            line(it.packageName)
            line(it.index)
            events(it.events)
            content(it.content, isTopLevel = true)
        }
    }

    companion object {
        const val FORMAT_VERSION = 1

        private const val FORMAT = "objc-export-units $FORMAT_VERSION"
        private const val LIST = "\u001E"

        private val Boolean.bit: Int
            get() = if (this) 1 else 0

        private fun ObjCExportNameRecord.encode(): String {
            val encodedClaim = claim?.let {
                listOf(
                    it.owner, "${it.isTopLevel.bit}${it.isConstructor.bit}${it.hasNoDispatchReceiver.bit}${it.isSetter.bit}",
                    it.kotlinName, it.extensionReceiverType, it.parameterTypes, it.bridge,
                ).joinToString(LIST)
            }.orEmpty()
            return listOf(kind.ordinal.toString(), key, name, encodedClaim).joinToString("\t") {
                it.replace('\t', ' ').replace('\n', ' ')
            }
        }

        private fun decodeRecord(value: String): ObjCExportNameRecord {
            val [kind, key, name, claim] = value.split('\t')
            val claimFields = claim.split(LIST)
            return ObjCExportNameRecord(
                ObjCExportNameKind.entries[kind.toInt()], key, name,
                if (claim.isEmpty()) null else ObjCExportMemberClaim(
                    owner = claimFields[0],
                    isTopLevel = claimFields[1][0] == '1',
                    isConstructor = claimFields[1][1] == '1',
                    hasNoDispatchReceiver = claimFields[1][2] == '1',
                    isSetter = claimFields[1][3] == '1',
                    kotlinName = claimFields[2],
                    extensionReceiverType = claimFields[3],
                    parameterTypes = claimFields[4],
                    bridge = claimFields[5],
                ),
            )
        }

        private fun ObjCGenericTypeDeclaration.encode(): String = when (this) {
            is ObjCGenericTypeRawDeclaration -> "R"
            is ObjCGenericTypeParameterDeclaration -> "P"
        } + variance.name + ":" + typeName

        private fun decodeTypeDeclaration(value: String): ObjCGenericTypeDeclaration {
            val separator = value.indexOf(':')
            val variance = ObjCVariance.valueOf(value.substring(1, separator))
            val typeName = value.substring(separator + 1)
            return when (value[0]) {
                'R' -> ObjCGenericTypeRawDeclaration(typeName, variance)
                'P' -> ObjCGenericTypeParameterDeclaration(typeName, variance)
                else -> error("Malformed generic type declaration '$value'")
            }
        }

        fun read(reader: BufferedReader): ObjCExportUnits {
            fun line(): String = reader.readLine() ?: error("Unexpected end of Objective-C export units")
            fun count(): Int = line().toInt()
            fun numbers(): List<Int> = line().split(' ').map { it.toInt() }
            fun <T> list(element: () -> T): List<T> = List(count()) { element() }

            fun events(): List<ObjCExportUnitEvent> = list {
                val value = line()
                ObjCExportUnitEvent(isEarly = value[0] == 'E', classId = value.substring(2))
            }

            fun content(): ObjCExportUnitContent = ObjCExportUnitContent.Lines(list(::line))

            require(line() == FORMAT) { "Unsupported Objective-C export units format" }
            val hasCollisions = line() == "1"
            val typeParameterClashes = list(::line)
            val classForwardDeclarations = list {
                val parts = line().split('\t')
                ObjCClassForwardDeclaration(parts[0], parts.drop(1).map(::decodeTypeDeclaration))
            }.toSet()
            val protocolForwardDeclarations = list(::line).toSet()
            val classClaims = list {
                val [classId, flags, supertypes] = line().split('\t')
                ObjCExportClassClaim(classId, supertypes.split(LIST).filterTo(mutableSetOf()) { it.isNotEmpty() }, isFinal = flags[0] == '1', isInterface = flags[1] == '1')
            }
            val names = list { decodeRecord(line()) }
            val classes = list {
                val classId = line()
                val [index, isInMainPass, isGenerated] = numbers()
                ObjCExportClassUnit(classId, index, isInMainPass == 1, isGenerated == 1, events(), content())
            }
            val categoryMembers = list {
                val classId = line()
                val className = line()
                val [isProperty, parameterCount, index] = numbers()
                val name = line()
                val signature = line()
                val packageName = line()
                val thrownEvents = events()
                val events = events()
                ObjCExportCategoryMember(
                    classId, className, ObjCExportMemberSortKey(isProperty == 1, name, parameterCount, signature), packageName, index,
                    thrownEvents, events, content(),
                )
            }
            val files = list {
                val hasName = line() == "1"
                val fileName = line().takeIf { hasName }
                val packageName = line()
                val index = count()
                ObjCExportFileUnit(fileName, packageName, index, events(), content())
            }
            return ObjCExportUnits(
                classes, categoryMembers, files, classForwardDeclarations, protocolForwardDeclarations,
                classClaims, names, hasCollisions, typeParameterClashes,
            )
        }
    }
}

@InternalKotlinNativeApi
class ObjCExportModuleUnits(val moduleIndex: Int, val units: ObjCExportUnits)

internal class ObjCExportHeaderAssembler(
    private val modules: List<ObjCExportModuleUnits>,
    liveEntries: List<Entries>,
    private val translateOnDemand: (String) -> ObjCExportClassUnit?,
    private val emitContent: (ObjCExportUnitContent) -> Unit,
    private val emitCategory: (String, List<ObjCExportUnitContent>) -> Unit,
) {
    class ClassEntry(val classId: String, val moduleIndex: Int, val index: Int, val translate: () -> Boolean)

    class CategoryMemberEntry(
        val classId: String,
        val packageName: String,
        val moduleIndex: Int,
        val index: Int,
        val sortKey: ObjCExportMemberSortKey,
        val className: () -> String,
        val exportThrown: () -> Unit,
        val content: () -> ObjCExportUnitContent,
    )

    class FileEntry(val fileName: String?, val packageName: String, val moduleIndex: Int, val index: Int, val emit: () -> Unit)

    class Entries(val classes: List<ClassEntry>, val categoryMembers: List<CategoryMemberEntry>, val files: List<FileEntry>)

    private val classUnits = mutableMapOf<String, ObjCExportClassUnit>()
    private val generated = mutableSetOf<String>()
    private val queue = linkedSetOf<String>()
    private val entries = modules.map(::cachedEntries) + liveEntries

    private fun cachedEntries(module: ObjCExportModuleUnits) = Entries(
        module.units.classes.filter { it.isInMainPass }.map { unit ->
            ClassEntry(unit.classId, module.moduleIndex, unit.index) {
                if (unit.isGenerated) emit(unit.classId) else emitContent(unit.content)
                false
            }
        },
        module.units.categoryMembers.map { member ->
            CategoryMemberEntry(
                member.classId, member.packageName, module.moduleIndex, member.index, member.sortKey,
                className = { member.className },
                exportThrown = { process(member.thrownEvents) },
                content = {
                    process(member.events)
                    member.content
                },
            )
        },
        module.units.files.map { file ->
            FileEntry(file.fileName, file.packageName, module.moduleIndex, file.index) {
                process(file.events)
                emitContent(file.content)
            }
        },
    )

    fun assemble() {
        modules.forEach { module ->
            module.units.classes.forEach { if (it.isGenerated) classUnits.putIfAbsent(it.classId, it) }
        }

        entries.flatMap { it.classes }.sortedWith(compareBy({ it.classId }, { it.moduleIndex }, { it.index })).forEach { entry ->
            if (entry.translate()) generated += entry.classId
        }

        entries.flatMap { it.categoryMembers }
            .groupBy { it.classId }
            .entries.sortedBy { it.key }
            .forEach { [classId, members] ->
                val ordered = members.sortedWith(compareBy({ it.packageName }, { it.moduleIndex }, { it.index }))
                emit(classId)
                val className = ordered.first().className()
                ordered.filter { !it.sortKey.isProperty }.forEach { it.exportThrown() }
                emitCategory(className, ordered.sortedBy { it.sortKey }.map { it.content() })
            }

        entries.flatMap { it.files }
            .sortedWith(compareBy({ it.fileName }, { it.packageName }, { it.moduleIndex }, { it.index }))
            .forEach { it.emit() }

        while (queue.isNotEmpty()) {
            val classId = queue.first()
            queue.remove(classId)
            emit(classId)
        }
    }

    private fun process(events: List<ObjCExportUnitEvent>) {
        events.forEach { if (it.isEarly) emit(it.classId) else reference(it.classId) }
    }

    fun reference(classId: String) {
        if (classId !in generated) queue += classId
    }

    fun emit(classId: String) {
        if (!generated.add(classId)) return
        val unit = classUnits[classId] ?: translateOnDemand(classId) ?: return
        process(unit.events)
        emitContent(unit.content)
    }
}
