@file:Suppress("UNCHECKED_CAST")

package hair.ir.nodes

import hair.ir.Session
import hair.utils.isEmpty
import kotlin.jvm.JvmName

abstract class SessionBase {
    val forms = mutableListOf<Form>()

    abstract val argUpdater: ArgumentUpdater

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

    fun wireToGraph(node: Node) {
        require(node in allNodes())
        if (node.inGraph) return
        node as NodeBase
        node.id = -node.id
        require(node.inGraph)
        for (arg in node.args) {
            arg?.addUse(node)
        }
    }

    fun unwireFromGraphAndDeregister(node: Node) {
        require(node.inGraph)
        require(node.uses.isEmpty())
        node as NodeBase
        node.id = -node.id
        require(!node.inGraph)

        // deregister (asymmetrical with wireToGraph)
        node.form.deregister(node)

        for (arg in node.args) {
            arg?.removeUse(node)
        }

        require(node !in allNodes())
    }
}

fun Node.unwireFromGraphAndDeregister() {
    session.unwireFromGraphAndDeregister(this)
}

/**
 * The scope of an IR modification, and the capability to build nodes and edit the IR within it.
 * Edits that restructure the control flow additionally require a [ControlEditor].
 */
interface NodeBuilder : ArgumentUpdater {
    val session: Session
    fun onNodeBuilt(node: Node): Node
}

/**
 * The scope of an IR modification that may restructure the control flow, and the capability that the operations
 * doing so require.
 *
 * It decides what may be done, not where: control-flow nodes are appended at a [ControlBuilder].
 */
interface ControlEditor : NodeBuilder
