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

    fun <N: Node> gvn(node: N): N {
        return node.ensureUnique() as N
    }

    fun register(node: Node) {
        require(node in allNodes())
        if (node.registered) return
        node as NodeBase
        node.id = -node.id
        require(node.registered)
        for (arg in node.args) {
            arg?.addUse(node)
        }
    }

    fun unregister(node: Node) {
        require(node.registered)
        require(node.uses.isEmpty())
        node as NodeBase
        node.id = -node.id
        require(!node.registered)

        // FIXME is not symmetrical with the register
        node.form.deregister(node)

        for (arg in node.args) {
            arg?.removeUse(node)
        }

        require(node !in allNodes())
    }
}

fun Node.deregister() {
    session.unregister(this)
}

//fun <N: Node> N.register(): N {
//    if (registered) return this // FIXME!!
//    require(!registered)
//    val uniq = ensureUnique() as N
//
//    if (uniq != this) {
//        require(uniq.registered)
//        return uniq
//    }
//
//    for (arg in args) {
//        arg?.addUse(this)
//    }
//
//    if (!this.registered) {
//        this as NodeBase
//        this.id = -this.id
//        require(this.registered)
//    }
//
//    return this
//}
//
//fun Node.deregister() {
//    require(registered)
//    require(uses.isEmpty())
//    this as NodeBase
//    id = -id
//    require(!registered)
//    form.deregister(this)
//    for (arg in args) {
//        // FIXME???
//        arg?.removeUse(this)
//    }
//}

// TODO
interface NodeBuilder {
    val session: Session
    fun onNodeBuilt(node: Node): Node
}
