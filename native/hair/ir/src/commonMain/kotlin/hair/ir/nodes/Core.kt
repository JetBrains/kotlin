@file:Suppress("UNCHECKED_CAST")

package hair.ir.nodes

import hair.ir.Session
import hair.utils.isEmpty
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

    if (uniq != this) {
        require(uniq.registered)
        return uniq
    }

    for (arg in args) {
        arg?.addUse(this)
    }

    if (!this.registered) {
        this as NodeBase
        this.id = -this.id
        require(this.registered)
    }

    return this
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

// TODO
interface NodeBuilder {
    val session: Session

    fun normalize(node: Node): Node
    fun <N: Node> register(node: N): N
}