package hair.ir.nodes

import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.text.set

class ArgsList(val host: Node, initialValues: List<Node?>) : Iterable<Node?> {
    internal val elements: MutableList<Node?> = initialValues.toMutableList()
    operator fun get(index: Int): Node = elements[index]!!
    fun iterator(starting: Int) = elements.listIterator(starting)
    override fun iterator() = iterator(0)
    operator fun contains(element: Node) = elements.contains(element)
}

class VarArgsList<N: Node>(val argsList: ArgsList, val firstVarargIndex: Int, val type: KClass<N>) : Iterable<N> {
    val size: Int = argsList.elements.size - firstVarargIndex // TODO make list expandable?
    val isEmpty: Boolean get() = size == 0
    val isNotEmpty: Boolean get() = !isEmpty
    operator fun get(index: Int): N = type.cast(argsList[index + firstVarargIndex])
    override fun iterator() = object : Iterator<N> {
        val baseIter = argsList.iterator(firstVarargIndex)
        override fun next(): N = type.cast(baseIter.next()!!)
        override fun hasNext(): Boolean = baseIter.hasNext()
    }
}

inline fun <reified N: Node> VarArgsList<N>.toTypedArray() = toList().toTypedArray()

interface ArgsUpdater {

    fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?)

    fun Node.updateArg(index: Int, oldValue: Node?, newValue: Node?, update: () -> Unit) {
        if (oldValue == newValue) return

        val oldArgs = args.toList()

        update()

        // TODO there is more to do ?

        if (newValue != null) {
            if (!newValue.registered) {
                newValue.register()
            }
            newValue.addUse(this)
        }

        // if oldValue is still in args, it's some other arg that has the same value
        if (oldValue != null && oldValue !in args) {
            oldValue.removeUse(this)
        }

        onArgUpdate(this, index, oldValue, newValue)

        val replacement = form.ensureUniqueAfterArgsUpdate(this, oldArgs)
        if (replacement != this) {
            this.replaceValueUses(replacement)
        }
    }

    fun Node.replaceValueUses(by: Node) {
        // TODO optimize, perhaps introduce abstraction of edge?
        for (use in this.uses.toList()) {
            val useEdges = use.args.toList().withIndex().filter { it.value == this }
            for ((argIndex, _) in useEdges) {
                use.args[argIndex] = by
            }
        }
    }

    operator fun ArgsList.set(index: Int, element: Node): Node? {
        val old = elements[index]
        host.updateArg(index, old, element) {
            elements[index] = element //.also { require(it == old) }
        }
        return old
    }

    operator fun <N : Node> VarArgsList<N>.set(index: Int, value: N) {
        argsList[index + firstVarargIndex] = value
    }
}
