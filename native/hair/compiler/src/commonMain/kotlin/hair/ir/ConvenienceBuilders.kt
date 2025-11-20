package hair.ir

import hair.ir.nodes.ArgsUpdater
import hair.ir.nodes.BlockEntry
import hair.ir.nodes.BlockExit
import hair.ir.nodes.ControlFlowBuilder
import hair.ir.nodes.Controlling
import hair.ir.nodes.Goto
import hair.ir.nodes.If
import hair.ir.nodes.Node
import hair.ir.nodes.NodeBuilder
import hair.ir.nodes.Throwing
import hair.ir.nodes.Unreachable
import hair.ir.nodes.set
import hair.sym.HairType.*
import hair.sym.Type
import hair.utils.ensuring

// return void
context(nodeBuilder: NodeBuilder)
fun ReturnVoid(control: Controlling?) = Return(control, UnitValue())

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReturnVoid() = Return(UnitValue())

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun IfExits(cond: Node): Pair<BlockExit, BlockExit> {
    val ifNode = If(cond)
    if (ifNode is Unreachable) return ifNode to ifNode
    ifNode as If
    val trueExit = TrueExit(ifNode)
    val falseExit = FalseExit(ifNode)
    return trueExit to falseExit
}


// CFG structures
private typealias BodyBuilder = context(NodeBuilder, ControlFlowBuilder) () -> Unit

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun branch(
    cond: Node,
    trueInit: BodyBuilder,
    falseInit: BodyBuilder
) {
    val (trueExit, falseExit) = IfExits(cond)

    BlockEntry(trueExit).ensuring { controlBuilder.lastControl == it }
    trueInit()
    val trueGoto = controlBuilder.lastControl?.let { Goto() }

    BlockEntry(falseExit).ensuring { controlBuilder.lastControl == it }
    falseInit()
    val falseGoto = controlBuilder.lastControl?.let { Goto() }

    BlockEntry(*listOfNotNull(trueGoto, falseGoto).toTypedArray())
}

context(_: NodeBuilder, _: ControlFlowBuilder)
fun <T> irBuilder(builder: context(NodeBuilder, ControlFlowBuilder) () -> T) = builder

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun branch(
    branches: List<Pair<(context(NodeBuilder, ControlFlowBuilder) () -> Node)?, BodyBuilder>>,
) {
    val exits = branches.map { (cond, body) ->
        if (cond == null) {
            body()
            if (controlBuilder.lastControl != null) Goto() else null
        } else {
            val (trueExit, falseExit) = IfExits(cond())

            BlockEntry(trueExit).ensuring { controlBuilder.lastControl == it }
            body()
            val trueGoto = if (controlBuilder.lastControl != null) Goto() else null

            BlockEntry(falseExit).ensuring { controlBuilder.lastControl == it }

            trueGoto
        }
    }

    if (exits.any { it != null }) {
        BlockEntry(*exits.filterNotNull().toTypedArray())
    }
}

// FIXME cond should be a builder
context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder, _: ArgsUpdater)
fun whileLoop(cond: Node, body: BodyBuilder) {
    val condBlock = BlockEntry(Goto(), null) as BlockEntry
    val (trueExit, falseExit) = IfExits(cond)

    BlockEntry(trueExit)
    body()
    condBlock.preds[1] = Goto()

    BlockEntry(falseExit)
}

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder, _: ArgsUpdater)
fun tryCatch(tryBody: BodyBuilder, catches: List<Pair<Type.Reference, context(NodeBuilder, ControlFlowBuilder) (Node) -> Unit>>, allCatcher: Type.Reference? = null) {
    val throwers = mutableListOf<Throwing>()
    val observingBuilder = object : NodeBuilder by nodeBuilder {
        override fun <N : Node> register(node: N): N {
            return nodeBuilder.register(node).also {
                if (it is Throwing) throwers += it
            }
        }
    }

    context(observingBuilder, controlBuilder) {
        tryBody()
    }
    val tryExit = if (controlBuilder.lastControl != null) Goto() else null

    val catchExits = mutableListOf<BlockExit>()
    if (catches.isNotEmpty()) {
        throwers.forEach { require(it.unwind == null) } // FIXME is this true?
        // TODO how do we handle nested try blocks?
        val unwinds = throwers.map { it.unwind ?: Unwind(it) }.toTypedArray()
        val handlerBlock = BlockEntry(*unwinds) as BlockEntry // FIXME cast?
        val exception = Catch(Phi(EXCEPTION)(handlerBlock, *unwinds))

        for ((type, catchBody) in catches) {
            // FIXME what do we know about the catchers order? Are all the type checks always possible?
            if (type == allCatcher) {
                catchBody(exception)
                controlBuilder.lastControl?.let { catchExits += Goto() }
                break
            }

            val (trueExit, falseExit) = IfExits(IsInstanceOf(type)(exception))

            BlockEntry(trueExit)
            catchBody(exception)

            controlBuilder.lastControl?.let { catchExits += Goto() }

            BlockEntry(falseExit)
        }

        controlBuilder.lastControl?.let { Throw(exception) }
    }

    val exits = listOfNotNull(tryExit) + catchExits
    if (exits.isNotEmpty()) {
        BlockEntry(*exits.toTypedArray())
    }
}
