package hair.ir

import hair.ir.nodes.*
import hair.opt.NormalizationImpl
import hair.utils.CoAppendableList

private val Session.baseNodeBuilder
    get() = BaseNodeBuilderImpl(this)

private val Session.normalizationWithBuilder: Pair<NodeBuilder, Normalization>
    get() {
        val normalization = NormalizationImpl(this)
        return NormalizingNodeBuilder(normalization, baseNodeBuilder).also {
            normalization.installBuilder(it)
        } to normalization
    }

private val Session.normalizingNodeBuilder: NodeBuilder
    get() = normalizationWithBuilder.first

// FIXME make a proper class
private fun Session.observingNodeBuilder(baseBuilder: NodeBuilder, observer: (Node) -> Unit) =
    object : ObservingNodeBuilder(baseBuilder) {
        override fun onNodeBuilt(node: Node) { observer(node) }
    }

class ArgUpdatesWatcher : ArgsModifier {
    val updatedNodes = CoAppendableList<Node>()

    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        updatedNodes.add(node)
    }
}

interface IrModifier : NodeBuilder, ArgsModifier

// FIXME generic?
class IrModifierImpl<NB: NodeBuilder, AM: ArgsModifier>(val nodeBuilder: NB, val argsModifier: AM) :
    IrModifier, NodeBuilder by nodeBuilder, ArgsModifier by argsModifier

sealed interface ControlFlowBuilder : NodeBuilder {
    val lastControl: Controlling?
}

class ControlFlowBuilderImpl(at: Controlling, baseBuilder: NodeBuilder) : ObservingNodeBuilder(baseBuilder), ControlFlowBuilder {

    private var _lastControl: Controlling? = at
    override val lastControl get() = _lastControl

    override fun onNodeBuilt(node: Node) {
        if (node is Controlled) {
            lastControl!!.nextControl = node
        }
        if (node is Controlling) {
            _lastControl = node
        }
        if (node is BlockEnd) {
            _lastControl = null
        }
    }
}

class InitialMemoryBuilder(val baseBuilder: ControlFlowBuilder) : ControlFlowBuilder by baseBuilder {
    private fun <T: MemoryAccess> attach(node: NodeBuilder.(Node) -> T): T = with (baseBuilder) {
        node(lastControl!!)
    }

    operator fun New.Form.invoke(): New =
        attach { this@invoke(it) }

    operator fun ReadFieldPinned.Form.invoke(obj: Node): ReadFieldPinned =
        attach { this@invoke(it, obj) }

    operator fun ReadGlobalPinned.Form.invoke(): ReadGlobalPinned =
        attach { this@invoke(it) }

    operator fun WriteField.Form.invoke(obj: Node, value: Node): WriteField =
        attach { this@invoke(it,obj, value) }

    operator fun WriteGlobal.Form.invoke(value: Node): WriteGlobal =
        attach { this@invoke(it, value) }

    operator fun StaticCall.Form.invoke(vararg callArgs: Node): StaticCall =
        attach { this@invoke(it, *callArgs) }

    fun Return(value: Node): Return = attach { Return(it, value) }
}

fun <T> Session.buildInitialIR(builderAction: InitialMemoryBuilder.() -> T): T {
    val controlBuilder = ControlFlowBuilderImpl(entryBlock, normalizingNodeBuilder)
    val builder = InitialMemoryBuilder(controlBuilder)
    return builder.run(builderAction)
}

fun <T> Session.modifyIR(builderAction: IrModifier.() -> T): T {
    val (normalBuilder, normalization) = normalizationWithBuilder
    val nodeBuilder = observingNodeBuilder(normalBuilder) {  } // TODO observe everything!!!
    val argsWatcher = ArgUpdatesWatcher()
    val modifier = IrModifierImpl(nodeBuilder, argsWatcher)
    val result = modifier.run(builderAction)

    for (node in argsWatcher.updatedNodes) {
        if (node is ControlFlow) continue
        if (!node.registered) continue
        require(node.uses.isNotEmpty())
        val replacement = normalization.normalize(node)
        if (replacement != node) {
            with (argsWatcher) {
                node.replaceValueUses(replacement)
            }
        }
    }
    return result
}

fun <T> Session.modifyControlFlow(at: Controlling, builderAction: ControlFlowBuilder.() -> T): T {
    val baseBuilder = observingNodeBuilder(normalizingNodeBuilder) {  } // TODO observe everything!!!
    val builder = ControlFlowBuilderImpl(at, baseBuilder)
    return builder.run(builderAction)
}

