package hair.ir.nodes

import hair.utils.listIterator
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.cast

@JvmInline
value class ArgsList(val host: NodeBase) : Iterable<Node?> {
    internal val elements: Array<Node?> get() = host.args_
    fun getOrNull(index: Int): Node? = elements[index]
    operator fun get(index: Int): Node = getOrNull(index)!!
    fun iterator(starting: Int) = elements.listIterator(starting)
    override fun iterator() = iterator(0)
    operator fun contains(element: Node) = elements.contains(element)
}

@Suppress("UNCHECKED_CAST")
class VarArgsList<N: Node>(val argsList: ArgsList, val firstVarargIndex: Int, val type: KClass<N>) : Iterable<N> {
    val size: Int = argsList.elements.size - firstVarargIndex // TODO make list expandable?
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty
    fun getOrNull(index: Int): N? = argsList.getOrNull(index + firstVarargIndex) as N?
    operator fun get(index: Int): N = getOrNull(index)!!
    override fun iterator() = object : Iterator<N> {
        val baseIter = argsList.iterator(firstVarargIndex)
        // TODO replace with unckecked cast?
        override fun next(): N = type.cast(baseIter.next()!!)
        override fun hasNext(): Boolean = baseIter.hasNext()
    }
    val withNulls: Iterable<N?> get() = object : Iterable<N?> {
        override fun iterator(): Iterator<N?> = object : Iterator<N?> {
            val baseIter = argsList.iterator(firstVarargIndex)
            override fun next(): N? = baseIter.next() as N?
            override fun hasNext(): Boolean = baseIter.hasNext()
        }
    }
}

inline fun <reified N: Node> VarArgsList<N>.toTypedArray(): Array<N> = toList().toTypedArray()

interface ArgsUpdater {
    fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?)
}

object SimpleArgsUpdater : ArgsUpdater {
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {}
}

context(argsUpdater: ArgsUpdater)
fun Node.replaceValueUses(by: Node) {
    // TODO optimize, perhaps introduce abstraction of edge?
    for (use in this.uses.toList()) {
        val drop = if (use is Controlled) 1 else 0
        val useEdges = use.args.withIndex().drop(drop).filter { it.value == this }
        for ((argIndex, _) in useEdges) {
            use.args[argIndex] = by
        }
    }
}

context(argsUpdater: ArgsUpdater)
private fun Node.updateArg(index: Int, oldValue: Node?, newValue: Node?, update: () -> Unit) {
    if (oldValue == newValue) return

    val oldArgs = args.toList()

    update()

    if (!registered) return
    // TODO there is more to do ?

    newValue?.addUse(this)
    oldValue?.removeUse(this)

    argsUpdater.onArgUpdate(this, index, oldValue, newValue)

    val replacement = form.ensureUniqueAfterArgsUpdate(this, oldArgs)
    if (replacement != this) {
        // FIXME what about control here?
        this.replaceValueUses(replacement)
    }
}

context(argsUpdater: ArgsUpdater)
operator fun ArgsList.set(index: Int, element: Node?): Node? {
    val old = elements[index]
    host.updateArg(index, old, element) {
        elements[index] = element
    }
    return old
}

// FIXME maybe it would be enough to have just add args for varargs?
// FIXME make safer. Use with care
context(argsUpdater: ArgsUpdater)
fun Node.replaceArgs(newArgs: Array<Node?>): Node {
    this as NodeBase
    val oldArgs = args_
    args_ = newArgs
    val argsCount = max(args_.size, newArgs.size)
    for (i in 0 until argsCount) {
        val oldValue = oldArgs.getOrNull(i)
        val newValue = newArgs.getOrNull(i)
        // FIXME duplicates updateArg
        if (oldValue != newValue) {
            newValue?.addUse(this)
            oldValue?.removeUse(this)
            argsUpdater.onArgUpdate(this, i, oldValue, newValue)
        }
    }

    val replacement = form.ensureUniqueAfterArgsUpdate(this, oldArgs.toList())
    return if (replacement != this) {
        // FIXME what about control here?
        this.replaceValueUses(replacement)
        replacement
    } else this
}

context(_: ArgsUpdater)
fun ArgsList.erase(index: Int): Node? = set(index, null)

context(argsUpdater: ArgsUpdater)
operator fun <N : Node> VarArgsList<N>.set(index: Int, value: N?) {
    argsList[index + firstVarargIndex] = value
}
