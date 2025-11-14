package hair.ir.nodes

import hair.ir.Session

abstract class Form(val session: Session) {
    abstract val nodes: Collection<Node>
    abstract val simpleName: String
    open val fullName: String get() = simpleName
    override fun toString() = fullName

    internal abstract fun ensureUnique(node: Node): Node
    internal abstract fun ensureUniqueAfterArgsUpdate(node: Node, oldArgs: List<Node?>): Node
    internal abstract fun deregister(node: Node)
}

abstract class ControlFlowForm(session: Session) : Form(session) {
    override val nodes = linkedSetOf<Node>()

    override fun ensureUnique(node: Node): Node {
        require(node.form == this)
        nodes.add(node)
        return node
    }

    override fun ensureUniqueAfterArgsUpdate(node: Node, oldArgs: List<Node?>): Node = node

    override fun deregister(node: Node) {
        nodes.remove(node).also { require(it) }
    }
}

abstract class ValueNumberedNodeForm(session: Session) : Form(session) {
    val uniqueNodes = linkedMapOf<List<Node?>, Node>()
    override val nodes get() = uniqueNodes.values

    override fun ensureUnique(node: Node): Node {
        require(node.form == this)
        if (node.args.any { it == null }) return node
        // FIXME this should be invalidated more often!!
        return uniqueNodes.getOrPut(node.args.toList()) { node }
    }

    override fun ensureUniqueAfterArgsUpdate(node: Node, oldArgs: List<Node?>): Node {
        require(node.form == this)
        if (oldArgs.all { it != null }) {
            uniqueNodes.remove(oldArgs)!!.also { require(it == node) }
        }
        if (node.args.any { it == null }) return node
        return ensureUnique(node)
    }

    override fun deregister(node: Node) {
        val args = node.args.toList()
        if (uniqueNodes[args] == node) {
            // FIXME not the prettiest thing
            //     It may happen that the node was value-numbered into another and we came here after replacing it's uses
            uniqueNodes.remove(args)
        }
    }
}

class SimpleControlFlowForm(session: Session, override val simpleName: String) : ControlFlowForm(session)
class SimpleValueForm(session: Session, override val simpleName: String) : ValueNumberedNodeForm(session)

class MetaForm(val session: Session, val simpleName: String) {
    private val forms = linkedMapOf<List<Any>, ParametrizedForm<*>>() // TODO use compact map?

    private fun ensureUnique(form: ParametrizedForm<*>): ParametrizedForm<*> {
        require(form.metaForm == this)
        return forms.getOrPut(form.args) {
            session.register(form as Form)
            form
        }
    }

    interface ParametrizedForm<F: ParametrizedForm<F>> {
        val metaForm: MetaForm
        val args: List<Any>

        val simpleName get() = metaForm.simpleName
        val fullName get() = "${simpleName}[${args.joinToString()}]"

        @Suppress("UNCHECKED_CAST")
        fun ensureFormUniq(): F {
            return metaForm.ensureUnique(this) as F
        }
    }

    abstract class ParametrisedControlFlowForm<F: ParametrisedControlFlowForm<F>>(override val metaForm: MetaForm)
        : ControlFlowForm(metaForm.session), ParametrizedForm<F> {
        override val simpleName: String get() = super.simpleName
        override val fullName: String get() = super<ParametrizedForm>.fullName
    }

    abstract class ParametrisedValueForm<F: ParametrisedValueForm<F>>(override val metaForm: MetaForm)
        : ValueNumberedNodeForm(metaForm.session), ParametrizedForm<F> {
        override val simpleName: String get() = super.simpleName
        override val fullName: String get() = super<ParametrizedForm>.fullName
    }

    // TODO Singleton form?
}
