/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import llvm.LLVMStoreSizeOfType
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.llvmType
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.backend.konan.lower.InnerClassLowering
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName

internal class OverriddenFunctionInfo(
        val function: IrSimpleFunction,
        val overriddenFunction: IrSimpleFunction
) {
    val needBridge: Boolean
        get() = function.target.needBridgeTo(overriddenFunction)

    val bridgeDirections: BridgeDirections
        get() = function.target.bridgeDirectionsTo(overriddenFunction)

    val canBeCalledVirtually: Boolean
        get() {
            if (overriddenFunction.isObjCClassMethod()) {
                return function.canObjCClassMethodBeCalledVirtually(overriddenFunction)
            }

            return overriddenFunction.isOverridable
        }

    val inheritsBridge: Boolean
        get() = !function.isReal
                && function.target.overrides(overriddenFunction)
                && function.bridgeDirectionsTo(overriddenFunction).allNotNeeded()

    fun getImplementation(context: Context): IrSimpleFunction? {

        val target = function.target
        val implementation = if (!needBridge)
            target
        else {
            val bridgeOwner = if (inheritsBridge) {
                target // Bridge is inherited from superclass.
            } else {
                function
            }
            context.specialDeclarationsFactory.getBridge(OverriddenFunctionInfo(bridgeOwner, overriddenFunction))
        }
        return if (implementation.modality == Modality.ABSTRACT) null else implementation
    }

    override fun toString(): String {
        return "(descriptor=$function, overriddenDescriptor=$overriddenFunction)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverriddenFunctionInfo) return false

        if (function != other.function) return false
        if (overriddenFunction != other.overriddenFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = function.hashCode()
        result = 31 * result + overriddenFunction.hashCode()
        return result
    }
}

internal class ClassGlobalHierarchyInfo(val classIdLo: Int, val classIdHi: Int,
                                        val interfaceId: Int, val interfaceColor: Int) {
    companion object {
        val DUMMY = ClassGlobalHierarchyInfo(0, 0, 0, 0)

        // 32-items table seems like a good threshold.
        val MAX_BITS_PER_COLOR = 5
    }
}

internal class GlobalHierarchyAnalysisResult(val bitsPerColor: Int)

internal class GlobalHierarchyAnalysis(val context: Context, val irModule: IrModuleFragment) {
    fun run() {
        /*
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
         * Interface call: *(itable[interfaceId & (size - 1)].vtable[methodIndex])(...)
         * Interface check: itable[interfaceId & (size - 1)].id == interfaceId
         *
         * Note that we have a fallback to a more conservative version if the size of an itable is too large:
         * just save all interface ids and vtables in sorted order and find the needed one with the binary search.
         * We can signal that using the sign bit of the type info's size field:
         *     if (size >= 0) { .. fast path .. }
         *     else binary_search(0, -size)
         */
        val interfaceColors = assignColorsToInterfaces()
        val maxColor = interfaceColors.values.maxOrNull() ?: 0
        var bitsPerColor = 0
        var x = maxColor
        while (x > 0) {
            ++bitsPerColor
            x /= 2
        }

        val maxInterfaceId = Int.MAX_VALUE shr bitsPerColor
        val colorCounts = IntArray(maxColor + 1)

        /*
         * Here's the explanation of what's happening here:
         * Given a tree we can traverse it with the DFS and save for each vertex two times:
         * the enter time (the first time we saw this vertex) and the exit time (the last time we saw it).
         * It turns out that if we assign then for each vertex the interval (enterTime, exitTime),
         * then the following claim holds for any two vertices v and w:
         * ----- v is ancestor of w iff interval(v) contains interval(w) ------
         * Now apply this idea to the classes hierarchy tree and we'll get a fast type check.
         *
         * And one more observation: for each pair of intervals they either don't intersect or
         * one contains the other. With that in mind, we can save in a type info only one end of an interval.
         */
        val root = context.irBuiltIns.anyClass.owner
        val immediateInheritors = mutableMapOf<IrClass, MutableList<IrClass>>()
        val allClasses = mutableListOf<IrClass>()
        irModule.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.isInterface) {
                    val color = interfaceColors[declaration]!!
                    // Numerate from 1 (reserve 0 for invalid value).
                    val interfaceId = ++colorCounts[color]
                    assert (interfaceId <= maxInterfaceId) {
                        "Unable to assign interface id to ${declaration.name}"
                    }
                    context.getLayoutBuilder(declaration).hierarchyInfo =
                            ClassGlobalHierarchyInfo(0, 0,
                                    color or (interfaceId shl bitsPerColor), color)
                } else {
                    allClasses += declaration
                    if (declaration != root) {
                        val superClass = declaration.getSuperClassNotAny() ?: root
                        val inheritors = immediateInheritors.getOrPut(superClass) { mutableListOf() }
                        inheritors.add(declaration)
                    }
                }
                super.visitClass(declaration)
            }
        })
        var time = 0

        fun dfs(irClass: IrClass) {
            ++time
            // Make the Any's interval's left border -1 in order to correctly generate classes for ObjC blocks.
            val enterTime = if (irClass == root) -1 else time
            immediateInheritors[irClass]?.forEach { dfs(it) }
            val exitTime = time
            context.getLayoutBuilder(irClass).hierarchyInfo = ClassGlobalHierarchyInfo(enterTime, exitTime, 0, 0)
        }

        dfs(root)

        context.globalHierarchyAnalysisResult = GlobalHierarchyAnalysisResult(bitsPerColor)
    }

    class InterfacesForbiddennessGraph(val nodes: List<IrClass>, val forbidden: List<List<Int>>) {

        fun computeColoringGreedy(): IntArray {
            val colors = IntArray(nodes.size) { -1 }
            var numberOfColors = 0
            val usedColors = BooleanArray(nodes.size)
            for (v in nodes.indices) {
                for (c in 0 until numberOfColors)
                    usedColors[c] = false
                for (u in forbidden[v])
                    if (colors[u] >= 0)
                        usedColors[colors[u]] = true
                var found = false
                for (c in 0 until numberOfColors)
                    if (!usedColors[c]) {
                        colors[v] = c
                        found = true
                        break
                    }
                if (!found)
                    colors[v] = numberOfColors++
            }
            return colors
        }

        companion object {
            fun build(irModuleFragment: IrModuleFragment): InterfacesForbiddennessGraph {
                val interfaceIndices = mutableMapOf<IrClass, Int>()
                val interfaces = mutableListOf<IrClass>()
                val forbidden = mutableListOf<MutableList<Int>>()
                irModuleFragment.acceptVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    fun registerInterface(iface: IrClass) {
                        interfaceIndices.getOrPut(iface) {
                            forbidden.add(mutableListOf())
                            interfaces.add(iface)
                            interfaces.size - 1
                        }
                    }

                    override fun visitClass(declaration: IrClass) {
                        if (declaration.isInterface)
                            registerInterface(declaration)
                        else {
                            val implementedInterfaces = declaration.implementedInterfaces
                            implementedInterfaces.forEach { registerInterface(it) }
                            for (i in 0 until implementedInterfaces.size)
                                for (j in i + 1 until implementedInterfaces.size) {
                                    val v = interfaceIndices[implementedInterfaces[i]]!!
                                    val u = interfaceIndices[implementedInterfaces[j]]!!
                                    forbidden[v].add(u)
                                    forbidden[u].add(v)
                                }
                        }
                        super.visitClass(declaration)
                    }
                })
                return InterfacesForbiddennessGraph(interfaces, forbidden)
            }
        }
    }

    private fun assignColorsToInterfaces(): Map<IrClass, Int> {
        val graph = InterfacesForbiddennessGraph.build(irModule)
        val coloring = graph.computeColoringGreedy()
        return graph.nodes.mapIndexed { v, irClass -> irClass to coloring[v] }.toMap()
    }
}

internal class ClassLayoutBuilder(val irClass: IrClass, val context: Context, val isLowered: Boolean) {
    val vtableEntries: List<OverriddenFunctionInfo> by lazy {
        assert(!irClass.isInterface)

        context.logMultiple {
            +""
            +"BUILDING vTable for ${irClass.render()}"
        }

        val superVtableEntries = if (irClass.isSpecialClassWithNoSupertypes()) {
            emptyList()
        } else {
            val superClass = irClass.getSuperClassNotAny() ?: context.ir.symbols.any.owner
            context.getLayoutBuilder(superClass).vtableEntries
        }

        val methods = irClass.sortedOverridableOrOverridingMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionInfo>()
        val overridenVtableSlots = mutableMapOf<IrSimpleFunction, OverriddenFunctionInfo>()

        context.logMultiple {
            +""
            +"SUPER vTable:"
            superVtableEntries.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()}" }

            +""
            +"METHODS:"
            methods.forEach { +"    ${it.render()}" }

            +""
            +"BUILDING INHERITED vTable"
        }

        val superVtableMap = superVtableEntries.groupBy { it.function }
        methods.forEach { overridingMethod ->
            overridingMethod.allOverriddenFunctions.forEach {
                val superMethods = superVtableMap[it]
                if (superMethods?.isNotEmpty() == true) {
                    newVtableSlots.add(OverriddenFunctionInfo(overridingMethod, it))
                    superMethods.forEach { superMethod ->
                        overridenVtableSlots[superMethod.overriddenFunction] =
                                OverriddenFunctionInfo(overridingMethod, superMethod.overriddenFunction)
                    }
                }
            }
        }
        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            overridenVtableSlots[superMethod.overriddenFunction]?.also {
                context.log { "Taking overridden ${superMethod.overriddenFunction.render()} -> ${it.function.render()}" }
            } ?: superMethod.also {
                context.log { "Taking super ${superMethod.overriddenFunction.render()} -> ${superMethod.function.render()}" }
            }
        }

        // Add all possible (descriptor, overriddenDescriptor) edges for now, redundant will be removed later.
        methods.mapTo(newVtableSlots) { OverriddenFunctionInfo(it, it) }

        val inheritedVtableSlotsSet = inheritedVtableSlots.map { it.function to it.bridgeDirections }.toSet()

        val filteredNewVtableSlots = newVtableSlots
            .filterNot { inheritedVtableSlotsSet.contains(it.function to it.bridgeDirections) }
            .distinctBy { it.function to it.bridgeDirections }
            .filter { it.function.isOverridable }

        context.logMultiple {
            +""
            +"INHERITED vTable slots:"
            inheritedVtableSlots.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()}" }

            +""
            +"MY OWN vTable slots:"
            filteredNewVtableSlots.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()} ${it.function}" }
            +"DONE vTable for ${irClass.render()}"
        }

        inheritedVtableSlots + filteredNewVtableSlots.sortedBy { it.overriddenFunction.uniqueId }
    }

    fun vtableIndex(function: IrSimpleFunction): Int {
        val bridgeDirections = function.target.bridgeDirectionsTo(function)
        val index = vtableEntries.indexOfFirst { it.function == function && it.bridgeDirections == bridgeDirections }
        if (index < 0) throw Error(function.render() + " $function " + " (${function.symbol.descriptor}) not in vtable of " + irClass.render())
        return index
    }

    val methodTableEntries: List<OverriddenFunctionInfo> by lazy {
        irClass.sortedOverridableOrOverridingMethods
                .flatMap { method -> method.allOverriddenFunctions.map { OverriddenFunctionInfo(method, it) } }
                .filter { it.canBeCalledVirtually }
                .distinctBy { it.overriddenFunction.uniqueId }
                .sortedBy { it.overriddenFunction.uniqueId }
        // TODO: probably method table should contain all accessible methods to improve binary compatibility
    }

    val interfaceTableEntries: List<IrSimpleFunction> by lazy {
        irClass.sortedOverridableOrOverridingMethods
                .filter { f ->
                    f.isReal || f.overriddenSymbols.any { OverriddenFunctionInfo(f, it.owner).needBridge }
                }
                .toList()
    }

    data class InterfaceTablePlace(val interfaceId: Int, val itableSize: Int, val methodIndex: Int) {
        companion object {
            val INVALID = InterfaceTablePlace(0, -1, -1)
        }
    }

    fun itablePlace(function: IrSimpleFunction): InterfaceTablePlace {
        assert (irClass.isInterface) { "An interface expected but was ${irClass.name}" }
        val itable = interfaceTableEntries
        val index = itable.indexOf(function)
        if (index >= 0)
            return InterfaceTablePlace(hierarchyInfo.interfaceId, itable.size, index)
        val superFunction = function.overriddenSymbols.first().owner
        return context.getLayoutBuilder(superFunction.parentAsClass).itablePlace(superFunction)
    }

    /**
     * All fields of the class instance.
     * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
     */
    val fields: List<IrField> by lazy {
        val superClass = irClass.getSuperClassNotAny() // TODO: what if Any has fields?
        val superFields = if (superClass != null) context.getLayoutBuilder(superClass).fields else emptyList()

        superFields + getDeclaredFields()
    }

    val associatedObjects by lazy {
        val result = mutableMapOf<IrClass, IrClass>()

        irClass.annotations.forEach {
            val irFile = irClass.getContainingFile()

            val annotationClass = (it.symbol.owner as? IrConstructor)?.constructedClass
                    ?: error(irFile, it, "unexpected annotation")

            if (annotationClass.hasAnnotation(RuntimeNames.associatedObjectKey)) {
                val argument = it.getValueArgument(0)

                val irClassReference = argument as? IrClassReference
                        ?: error(irFile, argument, "unexpected annotation argument")

                val associatedObject = irClassReference.symbol.owner

                if (associatedObject !is IrClass || !associatedObject.isObject) {
                    error(irFile, irClassReference, "argument is not a singleton")
                }

                if (annotationClass in result) {
                    error(
                            irFile,
                            it,
                            "duplicate value for ${annotationClass.name}, previous was ${result[annotationClass]?.name}"
                    )
                }

                result[annotationClass] = associatedObject
            }
        }

        result
    }

    lateinit var hierarchyInfo: ClassGlobalHierarchyInfo

    /**
     * Fields declared in the class.
     */
    private fun getDeclaredFields(): List<IrField> {
        val declarations: List<IrDeclaration> = if (irClass.isInner && !isLowered) {
            // Note: copying to avoid mutation of the original class.
            irClass.declarations.toMutableList()
                    .also { InnerClassLowering.addOuterThisField(it, irClass, context) }
        } else {
            irClass.declarations
        }

        val fields = declarations.mapNotNull {
            when (it) {
                is IrField -> it.takeIf { it.isReal }
                is IrProperty -> it.takeIf { it.isReal }?.backingField
                else -> null
            }
        }

        if (irClass.hasAnnotation(FqName.fromSegments(listOf("kotlin", "native", "internal", "NoReorderFields"))))
            return fields

        return fields.sortedByDescending{ LLVMStoreSizeOfType(context.llvm.runtime.targetData, it.type.llvmType(context)) }
    }

    private val IrClass.sortedOverridableOrOverridingMethods: List<IrSimpleFunction>
        get() =
            this.simpleFunctions()
                    .filter { it.isOverridableOrOverrides && it.bridgeTarget == null }
                    .sortedBy { it.uniqueId }

    private val functionIds = mutableMapOf<IrFunction, Long>()

    private val IrFunction.uniqueId get() = functionIds.getOrPut(this) { functionName.localHash.value }
}
