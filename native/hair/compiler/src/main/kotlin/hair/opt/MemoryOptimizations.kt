package hair.opt

import hair.sym.*
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*

/**
 * MemoryChain(n: MemoryOp) contains ALL operations that could modify the Location(n) (through any aliases)
 */

private class LazyMap<K, V> private constructor(val initialValue: (K) -> V, private val impl: MutableMap<K, V>) : MutableMap<K, V> by impl {
    constructor(initialValue: (K) -> V) : this(initialValue, mutableMapOf())

    override operator fun get(key: K): V = impl.getOrPut(key) { initialValue(key) }
    // TODO other funs?
}

data class Location(val field: MemoryLocation, val obj: Node? = null)

val Node.knkownFileds: Set<Field> get() =
    uses.filterIsInstance<InstanceFieldOp>().filter { it.obj == this }.map { it.field }.toSet()

val SessionBase.allLocations: List<Location> get() = allNodes<MemoryOp>().flatMap { it.accessedLocations }.toSet().toList()

val MemoryOp.accessedLocations: List<Location> get() = when (this) {
    is InstanceFieldOp -> listOf(Location(field, obj))
    is GlobalFieldOp -> listOf(Location(field, null))
}

val MemoryAccess.accessedLocations: List<Location> get() = when (this) {
    is MemoryOp -> accessedLocations
    is AnyNew -> knkownFileds.map { Location(it, this) }
    is StaticCall -> session.allLocations // FIXME filter news out and more
    is Return -> session.allLocations // FIXME invent special token?
}

fun Set<Location>.aliasGroups(aliasAnalysis: AliasAnalysis): Map<Location, Set<Location>> {
    return this.associateWith { seed ->
        this.filter { it.field == seed.field }
            .filter {
                if (seed.obj == null) it.obj == null
                else if (it.obj == null) true
                else aliasAnalysis.aliases(seed.obj, it.obj).isPossible()
            }
            .toSet()
    }
}


fun Session.buildMemoryGraph(aliasAnalysis: AliasAnalysis) {
    val locations = allNodes<MemoryAccess>().flatMap { it.accessedLocations }.toSet()

    val aliasGroups = locations.aliasGroups(aliasAnalysis)

    val locationVar = LazyMap { loc: Location ->
        Var(loc).also {
            val (initialValue, point) = when (val obj = loc.obj) {
                // TODO
//                is AnyNew -> obj at obj
//                is Escape -> obj at obj.owner
                else -> entryBlock at entryBlock
            }
            insertAfter(point) { AssignVar(it)(initialValue) }
        }
    }

    modifyIR {
        for (node in allNodes<MemoryAccess>().toList()) {
            val point = node as Spinal // TODO support floating reads??
            for (location in node.accessedLocations) {
                val ownAliasGroup = aliasGroups[location]!!

                // read memory of all aliases
                val prevAccess = groupMem(ownAliasGroup.map { loc ->
                    val inVar = locationVar[loc]
                    insertBefore(point) { ReadVar(inVar) }
                })
                node.lastLocationAccess = prevAccess

                // define own memory
                val outVar = locationVar[location]
                insertAfter(point) { AssignVar(outVar)(node) }
            }
        }
    }
    buildSSA()

    combIndistinctMemories()
}

fun Session.combIndistinctMemories() {

    for (start in allNodes<IndistinctMemory>().toList()) {
        val visits = LazyMap { _: Node -> 0 }
        val worklist = mutableListOf<Node>(start)
        fun next(node: Node) = when (node) {
            is IndistinctMemory -> node.inputs.toList()
            is MemoryAccess -> listOf(node.lastLocationAccess)
            else -> emptyList()
        }
        while (worklist.isNotEmpty()) {
            val node = worklist.removeLast()

            for (next in next(node)) {
                val prevVisits = visits[next]
                if (prevVisits == 0) worklist += next
                visits[next] = prevVisits + 1
            }
        }
        val replacementInputs = start.inputs.filter {
            require(visits[it] > 0)
            visits[it] == 1
        }
        require(replacementInputs.isNotEmpty())
        if (replacementInputs.size < start.inputs.size) {
            modifyIR {
                val replacement = if (replacementInputs.size == 1) replacementInputs.single() else groupMem(replacementInputs)
                start.replaceValueUses(replacement)
            }
        }
    }
}

// TODO move somewhere
data class WithPoint<out N: Node>(val node: N, val point: Controlling)
infix fun <N: Node, P: Controlling> N.at(point: P) = WithPoint(this, point)


private fun IrModifier.groupMem(mems: Collection<Node>) =
    if (mems.size == 1) mems.single()
    else IndistinctMemory(*mems.toTypedArray())

