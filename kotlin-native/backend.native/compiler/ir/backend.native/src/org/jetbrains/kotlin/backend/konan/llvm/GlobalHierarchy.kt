/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.backend.konan.ir.isAbstract
import org.jetbrains.kotlin.backend.konan.isCache
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.classHierarchyIdsSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.interfaceIdSymbolName
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassHierarchy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * The class hierarchy of the whole program, assembled from the pieces the separate compilations contributed:
 * every cache it is linked against brings its own [records], and the module being compiled brings the rest.
 */
class ClassHierarchy(records: List<SerializedClassHierarchy>, anyName: String) {

    val any: ClassEntry

    val entries: List<ClassEntry>

    class ClassEntry(
            val name: String,
            val isInterface: Boolean,
            /** The number of methods in the vtable of this interface; zero for a class. */
            val interfaceVTableSize: Int = 0,
            /** Whether this class owns an interface lookup table. */
            val hasInterfaceLookupTable: Boolean = false,
    ) {
        var superClass: ClassEntry? = null
            internal set
        var interfaces: List<ClassEntry> = emptyList()
            internal set

        override fun equals(other: Any?): Boolean =
                this === other || (other is ClassEntry && other.name == name)
        override fun hashCode(): Int = name.hashCode()
        override fun toString(): String = name

        val directSupertypes: List<ClassEntry>
            get() = listOfNotNull(superClass) + interfaces
    }

    init {
        val entriesByName = records.associateBy(
                { it.className },
                { ClassEntry(it.className, it.isInterface, it.interfaceVTableSize, it.hasInterfaceLookupTable) }
        ).toMutableMap()

        // Any is never recorded as a superclass, so it is materialized here instead, and every class
        // that has no superclass of its own ends up inheriting from it. Interfaces stay parentless.
        any = entriesByName.getOrPut(anyName) { ClassEntry(anyName, isInterface = false) }

        fun entryOf(name: String) = entriesByName[name]
                ?: error("No class hierarchy record for $name, referenced as a supertype")

        // One pass suffices: every name a record refers to is itself recorded, and so already in the map.
        for (record in records) {
            val entry = entriesByName.getValue(record.className)
            entry.superClass = when {
                record.superClassName != null -> entryOf(record.superClassName)
                entry === any || entry.isInterface -> null
                else -> any
            }
            entry.interfaces = record.interfaceNames.map { entryOf(it) }
        }

        entries = entriesByName.values.toList()
    }

    private val directSubs: Map<ClassEntry, List<ClassEntry>> = entries
            .flatMap { sub -> sub.directSupertypes.map { it to sub } }
            .groupBy({ it.first }, { it.second })

    private val allSupers: Map<ClassEntry, Set<ClassEntry>> = closure { it.directSupertypes }
    private val allSubs: Map<ClassEntry, Set<ClassEntry>> = closure { directSubtypes(it) }

    private fun closure(next: (ClassEntry) -> List<ClassEntry>): Map<ClassEntry, Set<ClassEntry>> {
        val memo = HashMap<ClassEntry, Set<ClassEntry>>()
        fun visit(e: ClassEntry): Set<ClassEntry> = memo.getOrPut(e) {
            val acc = LinkedHashSet<ClassEntry>()
            for (n in next(e)) { acc.add(n); acc.addAll(visit(n)) }
            acc
        }
        entries.forEach(::visit)
        return memo
    }

    fun directSubtypes(cls: ClassEntry): List<ClassEntry> = directSubs[cls] ?: emptyList()

    /** Transitive supertypes, excluding [cls] itself. */
    fun allSupertypes(cls: ClassEntry): Set<ClassEntry> = allSupers[cls] ?: emptySet()
}


internal data class ClassIdInterval(val classIdLo: Int, val classIdHi: Int)

/** What an interface takes in the lookup table of every class implementing it. */
internal class InterfaceInfo(val id: Int, val vtableSize: Int)

internal class GlobalHierarchyAnalysisResult(
        val classIds: Map<String, ClassIdInterval>,
        val interfaces: Map<String, InterfaceInfo>,
        val implementedInterfaces: Map<String, List<String>>,
        val bitsPerColor: Int,
)


/**
 * Numbers the classes and the interfaces of the whole program, so that a type check becomes a range test and
 * an interface lookup a single indexed load.
 *
 * The algorithm for class type check:
 * Given a tree we can traverse it with the DFS and save for each vertex two times:
 * the enter time (the first time we saw this vertex) and the exit time (the last time we saw it).
 * It turns out that if we assign then for each vertex the interval (enterTime, exitTime),
 * then the following claim holds for any two vertices v and w:
 *     "v is ancestor of w" iff "interval(v) contains interval(w)"
 * Now apply this idea to the classes hierarchy tree, and we'll get a fast type check.
 *
 * The algorithm for fast interface call and check:
 * Consider the following graph: the vertices are interfaces and two interfaces are
 * connected with an edge if there exists a class which inherits both of them.
 * Now find a proper vertex-coloring of that graph (such that no edge connects vertices of same color).
 * Assign to each interface a unique id in such a way that its color is stored in the lower bits of its id.
 * Assuming the number of colors used is reasonably small build then a perfect hash table for each class:
 *     for each interfaceId inherited: itable[interfaceId % size] == interfaceId
 * Since we store the color in the lower bits the division can be replaced with (interfaceId & (size - 1)).
 * This is indeed a perfect hash table by construction of the coloring of the interface graph.
 * Now to perform an interface call store in all itables pointers to vtables of that particular interface.
 * Interface call: `*(itable[interfaceId & (size - 1)].vtable[methodIndex])(...)`
 * Interface check: `itable[interfaceId & (size - 1)].id == interfaceId`
 *
 * Note that we have a fallback to a more conservative version if the size of an itable is too large:
 * just save all interface ids and vtables in sorted order and find the needed one with the binary search.
 * We can signal that using the sign bit of the size field of the interface table:
 * ```
 *     if (size >= 0) { .. fast path .. }
 *     else binary_search(0, -size)
 * ```
 */
internal class GlobalHierarchyAnalysis(val context: NativeBackendContext, val irModule: IrModuleFragment) {

    fun run() {
        val hierarchy = context.buildWholeProgramClassHierarchy(irModule)
        val [interfaces, bitsPerColor] = numberInterfaces(hierarchy)

        context.globalHierarchyAnalysisResult = GlobalHierarchyAnalysisResult(
                classIds = numberClasses(hierarchy),
                interfaces = interfaces,
                // The layout of an interface table is keyed by the ids above, so it can only be decided here, once.
                implementedInterfaces = hierarchy.entries.filter { it.hasInterfaceLookupTable }.associate { entry ->
                    entry.name to hierarchy.allSupertypes(entry).filter { it.isInterface }.map { it.name }
                },
                bitsPerColor = bitsPerColor,
        )
    }

    /** Only classes: an interval numbering needs single inheritance, so the interfaces get colored ids instead. */
    private fun numberClasses(hierarchy: ClassHierarchy): Map<String, ClassIdInterval> {
        val classIds = mutableMapOf<String, ClassIdInterval>()
        var time = 0

        fun dfs(classEntry: ClassHierarchy.ClassEntry) {
            ++time
            // Make the Any's interval's left border -1 in order to correctly generate classes for ObjC blocks.
            val enterTime = if (classEntry == hierarchy.any) -1 else time
            hierarchy.directSubtypes(classEntry).forEach { if (!it.isInterface) dfs(it) }
            classIds[classEntry.name] = ClassIdInterval(enterTime, time)
        }

        dfs(hierarchy.any)
        return classIds
    }

    /** Assigns every interface an id holding its color in the lower bits, and returns how many bits that took. */
    private fun numberInterfaces(hierarchy: ClassHierarchy): Pair<Map<String, InterfaceInfo>, Int> {
        val colors = colorInterfaces(hierarchy)
        val maxColor = colors.values.maxOrNull() ?: 0
        var bitsPerColor = 0
        var x = maxColor
        while (x > 0) {
            ++bitsPerColor
            x /= 2
        }

        val maxInterfaceId = Int.MAX_VALUE shr bitsPerColor
        val colorCounts = IntArray(maxColor + 1)
        val interfaces = mutableMapOf<String, InterfaceInfo>()
        for ([iface, color] in colors) {
            // Numerate from 1 (reserve 0 for invalid value).
            val interfaceId = ++colorCounts[color]
            require(interfaceId <= maxInterfaceId) { "Unable to assign interface id to ${iface.name}" }
            interfaces[iface.name] = InterfaceInfo(color or (interfaceId shl bitsPerColor), iface.interfaceVTableSize)
        }

        return Pair(interfaces, bitsPerColor)
    }


    /**
     * A proper coloring of the graph whose vertices are the interfaces and whose edges connect the ones some class implements together.
     */
    private fun colorInterfaces(hierarchy: ClassHierarchy): Map<ClassHierarchy.ClassEntry, Int> {
        val interfaces = hierarchy.entries.filter { it.isInterface }
        val indices = interfaces.withIndex().associate { (index, value) -> value to index }

        val conflicts = Array(interfaces.size) { mutableSetOf<Int>() }
        for (cls in hierarchy.entries) {
            if (cls.isInterface) continue
            val implemented = hierarchy.allSupertypes(cls).filter { it.isInterface }.map { indices.getValue(it) }
            for (i in implemented.indices)
                for (j in i + 1 until implemented.size) {
                    conflicts[implemented[i]] += implemented[j]
                    conflicts[implemented[j]] += implemented[i]
                }
        }

        // Greedy coloring: take the first color no already colored conflicting interface has taken.
        val colors = IntArray(interfaces.size) { -1 }
        var numberOfColors = 0
        val used = BooleanArray(interfaces.size)
        for (v in interfaces.indices) {
            for (c in 0 until numberOfColors)
                used[c] = false
            for (u in conflicts[v])
                if (colors[u] >= 0)
                    used[colors[u]] = true
            colors[v] = (0 until numberOfColors).firstOrNull { !used[it] } ?: numberOfColors++
        }

        return interfaces.indices.associate { interfaces[it] to colors[it] }
    }
}


// 32-items table seems like a good threshold.
internal const val MAX_BITS_PER_COLOR = 5

/** An interface a class implements, along with what placing it into that class's lookup table takes. */
internal class InterfaceTableEntry(
        val interfaceName: String,
        val interfaceId: Int,
        val vtableSize: Int,
)

internal val NativeBackendContext.hierarchyWillBeLaidOutByFinalLink: Boolean
    get() = shouldOptimize() && with(config.cachedLibraries) {
        config.produce.isCache || hasStaticCaches || hasDynamicCaches
    }

internal fun NativeBackendContext.globalHierarchyInterfaceId(irClass: IrClass): Int =
        globalHierarchyAnalysisResult.interfaces.getValue(irClass.crossModuleName()).id

internal fun NativeBackendContext.globalHierarchyClassIdInterval(irClass: IrClass): ClassIdInterval =
        globalHierarchyAnalysisResult.classIds.getValue(irClass.crossModuleName())

/** The id of an interface where there is no numbering to take one from (e.g. in debug builds); the runtime compares these hashes. */
internal val IrClass.backupInterfaceId: Int
    get() = localHash(fqNameForIrSerialization.asString().toByteArray()).toInt()

internal val IrClass.needsInterfaceLookupTable: Boolean
    get() = !isInterface && !isAbstract() && !isObjCClass()


internal fun IrClass.buildClassHierarchyRecord(context: NativeBackendContext) = SerializedClassHierarchy(
        className = crossModuleName(),
        isInterface = isInterface,
        superClassName = getSuperClassNotAny()?.crossModuleName(),
        interfaceNames = superTypes.mapNotNull { it.classOrNull?.owner }
                .filter { it.isInterface }
                .map { it.crossModuleName() },
        interfaceVTableSize = if (isInterface) context.getLayoutBuilder(this).interfaceVTableEntries.size else 0,
        hasInterfaceLookupTable = needsInterfaceLookupTable,
)

/** The hierarchy piece contributed by [this] element, including the classes declared inside function bodies. */
internal fun IrElement.buildClassHierarchyRecords(context: NativeBackendContext): List<SerializedClassHierarchy> {
    val records = mutableListOf<SerializedClassHierarchy>()
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            records.add(declaration.buildClassHierarchyRecord(context))
            super.visitClass(declaration)
        }
    })
    return records
}

/** The hierarchy of the whole program: this module, plus the piece every linked cache contributed. */
internal fun NativeBackendContext.buildWholeProgramClassHierarchy(irModule: IrModuleFragment) = ClassHierarchy(
        irModule.buildClassHierarchyRecords(this) + config.linkedCaches.flatMap { it.serializedClassHierarchy },
        anyName = irBuiltIns.anyClass.owner.crossModuleName(),
)

/**
 * Emits everything the caches only refer to: the ids of every class and interface of the program, and the
 * interface lookup table of every class that owns one.
 */
internal fun ContextUtils.createGlobalHierarchyStructures() {
    if (!context.hasGlobalHierarchyAnalysis() || !context.hierarchyWillBeLaidOutByFinalLink) return
    val result = context.globalHierarchyAnalysisResult

    fun emitConstant(symbolName: String, type: LLVMTypeRef, initializer: ConstValue) {
        val global = staticData.getOrCreateGlobal(type, symbolName, isExported = true)
        global.setInitializer(initializer)
        global.setConstant(true)
        llvm.usedGlobals += global.llvmGlobal
    }

    for ([className, interval] in result.classIds) {
        emitConstant(
                classHierarchyIdsSymbolName(className),
                runtime.classHierarchyIdsType,
                Struct(
                        runtime.classHierarchyIdsType,
                        llvm.constInt32(interval.classIdLo), llvm.constInt32(interval.classIdHi)
                )
        )
    }
    for ([interfaceName, info] in result.interfaces) {
        emitConstant(interfaceIdSymbolName(interfaceName), llvm.int32Type, llvm.constInt32(info.id))
    }
    result.implementedInterfaces.forEach { [className, interfaceNames] ->
        createInterfaceLookupTable(className, interfaceNames.map { name ->
            val info = result.interfaces.getValue(name)
            InterfaceTableEntry(name, info.id, info.vtableSize)
        })
    }
}
