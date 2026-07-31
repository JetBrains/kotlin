package hair.ir

import hair.ir.nodes.*
import hair.sym.ArithmeticType
import hair.sym.HairClass
import hair.sym.HairType
import hair.sym.HairType.*
import hair.utils.ensuring

context(nodeBuilder: NodeBuilder)
fun ReturnVoid(control: Controlling?) = Return(control, UnitValue())

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReturnVoid() = Return(UnitValue())

context(nodeBuilder: NodeBuilder)
fun Const(type: ArithmeticType, value: Number) = Const(type.toHairType(), value)

context(nodeBuilder: NodeBuilder)
fun Const(type: HairType, value: Number) = when (type) {
    BYTE -> Const(value.toByte())
    SHORT -> Const(value.toShort())
    INT -> Const(value.toInt())
    LONG -> Const(value.toLong())
    FLOAT -> Const(value.toFloat())
    DOUBLE -> Const(value.toDouble())
    else -> error("Should not reach here $value (${value::class.simpleName})")
}

// FIXME make Unreachable value-numbered
context(controlBuilder: ControlFlowBuilder)
fun Session.unreachable() = controlBuilder.at(unreachable)


context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun IfExits(cond: Node): Pair<BlockExit, BlockExit> {
    val ifNode = If(cond)
    if (ifNode is Unreachable) return ifNode to ifNode
    ifNode as If
    val trueExit = TrueExit(ifNode)
    val falseExit = FalseExit(ifNode)
    return trueExit to falseExit
}

context(nodeBuilder: NodeBuilder)
fun Phi(block: Controlling, vararg inputs: ValueAndExit): Node = when (block) {
    is BlockEntry -> {
        val joinedValues = block.preds.map { exit ->
            if (exit is Unreachable) NoValue()
            else inputs.single { it.exit == exit }.value
        }.toTypedArray()
        Phi(block, *joinedValues)
    }
    else -> inputs.single().value
}

context(nodeBuilder: NodeBuilder)
fun Phi(block: Controlling, vararg inputs: Pair<BlockExit, Node>): Node = when (block) {
    is BlockEntry -> {
        val joinedValues = block.preds.map { exit ->
            if (exit is Unreachable) NoValue()
            else inputs.single { it.first == exit }.second
        }.toTypedArray()
        Phi(block, *joinedValues)
    }
    else -> inputs.single().second
}

context(_: NodeBuilder, _: ControlFlowBuilder)
fun Session.breakControlFlowWithUnreachable() {
    // terminate existing control flow with a block end
    Halt()
    // everythig that comes after is unreachable
    unreachable()
}

// CFG structures
typealias ExprBuilder = context(NodeBuilder, ControlFlowBuilder) () -> Node

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun branch(
    cond: Node,
    trueInit: context(NodeBuilder, ControlFlowBuilder) () -> Unit,
    falseInit: context(NodeBuilder, ControlFlowBuilder) () -> Unit
) {
    val condBuilder: ExprBuilder = { cond }
    branch(listOf(
        condBuilder to { trueInit(); NoValue() },
        null to { falseInit(); NoValue() }
    ))
}

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun branch(branches: List<Pair<ExprBuilder?, ExprBuilder>>): Node {
    require(branches.isNotEmpty())

    val exitsAndValues = branches.map { [cond, body] ->
        if (cond == null) {
            val value = body()
            val exit = Goto()
            exit to value
        } else {
            val [trueExit, falseExit] = IfExits(cond())

            BlockEntry(trueExit).ensuring { controlBuilder.lastControl == it }
            val value = body()
            val trueGoto = Goto()

            BlockEntry(falseExit).ensuring { controlBuilder.lastControl == it }

            trueGoto to value
        }
    }

    val [exits, values] = exitsAndValues.unzip()

    // TODO maybe short-cut here to unreachable / NoValue()
    val merge = BlockEntry(*exits.toTypedArray())
    val phi = Phi(merge, *exitsAndValues.toTypedArray())

    return phi
}

// FIXME cond should be a builder
context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder, _: ArgumentUpdaterBase)
fun whileLoop(cond: Node, body: context(NodeBuilder, ControlFlowBuilder) () -> Unit) {
    val condBlock = BlockEntry(Goto(), null) as BlockEntry
    val [trueExit, falseExit] = IfExits(cond)

    BlockEntry(trueExit)
    body()
    condBlock.preds[1] = Goto()

    BlockEntry(falseExit)
}

typealias CatchBuilder = context(NodeBuilder, ControlFlowBuilder) (Node) -> Node

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder, _: ArgumentUpdaterBase)
fun tryCatch(
    tryBody: ExprBuilder,
    catches: List<Pair<HairClass, CatchBuilder>>,
): Node {
    val throwers = mutableListOf<Throwing>()
    val throwersCollector = object : NodeBuilder by nodeBuilder {
        override fun onNodeBuilt(node: Node): Node {
            return nodeBuilder.onNodeBuilt(node).also {
                if (it is Throwing) throwers += it
            }
        }
    }

    val [tryExit, tryResult] = context(throwersCollector, controlBuilder) {
        require(contextOf<NodeBuilder>() == throwersCollector)
        val result = tryBody()
        Goto() to result
    }

    if (catches.isNotEmpty() && throwers.isNotEmpty()) {
        val unwinds = throwers.map {
            require(it.unwind == null)
            Unwind(it)
        }.toTypedArray()

        val handlerBlock = BlockEntry(*unwinds) as BlockEntry
        val exception = Catch(handlerBlock)

        // TOOD specially handle catch(Throwable)?
        val branches = catches.map { [catchType, catchBuilder] ->
            val cond: ExprBuilder = { IsInstanceOf(catchType)(exception) }
            val branchBuilder: ExprBuilder = { catchBuilder(exception) }
            cond to branchBuilder
        }
        val rethrow: ExprBuilder = {
            Throw(exception)
            NoValue()
        }

        val catchesResult = branch(branches + listOf(null to rethrow))
        val catchesExit = Goto()
        val merge = BlockEntry(tryExit, catchesExit)
        return Phi(merge, tryExit to tryResult, catchesExit to catchesResult)
    }

    return tryResult
}
