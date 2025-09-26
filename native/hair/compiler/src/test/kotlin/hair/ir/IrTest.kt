package hair.ir

import hair.compilation.Compilation
import hair.ir.nodes.*

interface IrTest {
    fun withTestSession(test: Session.() -> Unit) {
        Session().test()
    }

    fun testCompilation(test: Compilation.() -> Unit) {
        Compilation().test()
    }

    fun ControlFlowBuilder.branch(
        cond: Node,
        trueInit: ControlFlowBuilder.() -> Unit,
        falseInit: ControlFlowBuilder.() -> Unit
    ): If {
        val if_ = If(cond)

        Block().also { if_.trueExit = it }
        require(lastControl is Block)
        trueInit(this)
        val trueExit = lastControl?.let { Goto() }

        Block().also { if_.falseExit = it }
        falseInit(this)
        val falseExit = lastControl?.let { Goto() }

        if (trueExit != null || falseExit != null) {
            Block().also { merge ->
                trueExit?.let { it.exit = merge }
                falseExit?.let { it.exit = merge }
            }
        }

        return if_
    }

    fun ControlFlowBuilder.whileLoop(cond: Node, bodyInit: ControlFlowBuilder.() -> Unit): Block {
        val goto = Goto()
        val condBlock = Block().also { goto.exit = it }
        val if_ = If(cond)

        Block().also { if_.trueExit = it }
        bodyInit(this)
        val trueExit = Goto()
        trueExit.exit = condBlock

        Block().also { if_.falseExit = it }

        return condBlock
    }
}