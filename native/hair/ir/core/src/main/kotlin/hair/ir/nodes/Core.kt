@file:Suppress("UNCHECKED_CAST")

package hair.ir.nodes

import kotlin.jvm.JvmName

interface Normalization {
    fun normalize(node: Node): Node
}

abstract class SessionBase {
    val forms = mutableListOf<Form>()

    fun allNodes() = forms.asSequence().flatMap { it.nodes }
    @JvmName("allNodesOfType")
    inline fun <reified N : Node> allNodes(): Sequence<N> = allNodes().filterIsInstance<N>() // FIXME invalidatable?

    fun register(form: Form) {
        forms.add(form)
    }

    var nextNodeId = 1
}

fun <N: Node> N.register(): N {
    if (registered) return this // FIXME!!
    require(!registered)
    val uniq = ensureUnique() as N

    for (arg in args) {
        // FIXME???
        arg?.addUse(uniq)
    }

    if (!uniq.registered) {
        uniq as NodeBase
        uniq.id = -uniq.id
        require(uniq.registered)
    }

    return uniq
}

fun Node.deregister() {
    require(registered)
    require(uses.isEmpty())
    this as NodeBase
    id = -id
    require(!registered)
    form.deregister(this)
    for (arg in args) {
        // FIXME???
        arg?.removeUse(this)
    }
}
