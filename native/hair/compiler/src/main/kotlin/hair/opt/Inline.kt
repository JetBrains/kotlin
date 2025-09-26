package hair.opt

import hair.compilation.Compilation
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.shouldNotReachHere

fun Compilation.inline(call: StaticCall) {
    val callerSession = call.session as Session // FIXME obtain compilation by session?
    val calleeCompilation = getCompilation(call.function)

//    with (callerSession) {
//        modifyIR {
//            // FIXME find a way not to write it manually
//
//            val nodeCloner = object : NodeVisitor<Node>() {
//                val calleeNodeClones = mutableMapOf<Node, Node>()
//
//                fun cloneNode(node: Node): Node {
//                    require(node.session == calleeCompilation.session)
//                    return calleeNodeClones.getOrPut(node) {
//                        node.accept(this)
//                    }
//                }
//
//                override fun visitNode(node: Node): Node = shouldNotReachHere(node)
//
//                override fun visitParam(node: Param) = call.callArgs[node.number]
//                override fun visitReturn(node: Return) = Goto() // FIXME lost lastLocationAccess chain
//
//                override fun visitNoValue(node: NoValue) = NoValue()
//                override fun visitPlaceholder(node: Placeholder) = Placeholder(node.tag)(*node.inputs.map { cloneNode(it) }.toTypedArray())
//                override fun visitUse(node: Use) = Use(cloneNode(node.value))
//                override fun visitGoto(node: Goto) = Goto()
//                override fun visitIf(node: If) = If(cloneNode(node.condition))
//                override fun visitHalt(node: Halt) = Halt()
//                override fun visitBlock(node: Block) = Block()
//                override fun visitReadVar(node: ReadVar) = ReadVar(node.variable)
//                override fun visitAssignVar(node: AssignVar) = AssignVar(node.variable)(cloneNode(node.assignedValue))
//                override fun visitPhi(node: Phi) = Phi(node.block, *node.joinedValues.map { cloneNode(it) }.toTypedArray())
//                override fun visitConst(node: ConstInt) = ConstInt(node.value)
//                override fun visitAdd(node: Add) = Add(cloneNode(node.lhs), cloneNode(node.rhs))
//                override fun visitSub(node: Sub) = Sub(cloneNode(node.lhs), cloneNode(node.rhs))
//                override fun visitMul(node: Mul) = Mul(cloneNode(node.lhs), cloneNode(node.rhs))
//                override fun visitDiv(node: Div) = Div(cloneNode(node.lhs), cloneNode(node.rhs))
//                override fun visitRem(node: Rem) = Rem(cloneNode(node.lhs), cloneNode(node.rhs))
//                override fun visitNew(node: New) = New(node.type)(cloneNode(node.lastLocationAccess))
//                override fun visitReadFieldPinned(node: ReadFieldPinned) = ReadFieldPinned(node.field)(cloneNode(node.lastLocationAccess), cloneNode(node.obj))
//                override fun visitReadGlobalPinned(node: ReadGlobalPinned) = ReadGlobalPinned(node.field)(cloneNode(node.lastLocationAccess))
//                override fun visitWriteField(node: WriteField) = WriteField(node.field)(cloneNode(node.lastLocationAccess), cloneNode(node.obj), cloneNode(node.value))
//                override fun visitWriteGlobal(node: WriteGlobal) = WriteGlobal(node.field)(cloneNode(node.lastLocationAccess), cloneNode(node.value))
//                override fun visitIndistinctMemory(node: IndistinctMemory) = IndistinctMemory(*node.inputs.map { cloneNode(it) }.toTypedArray())
//                override fun visitUnknown(node: Unknown) = Unknown()
//                override fun visitEscape(node: Escape) = Escape(cloneNode(node.owner) as ControlFlow, cloneNode(node.origin), cloneNode(node.into))
//                override fun visitOverwrite(node: Overwrite) = Overwrite(cloneNode(node.owner) as ControlFlow, cloneNode(node.origin))
//                override fun visitNeqFilter(node: NeqFilter) = NeqFilter(cloneNode(node.owner) as ControlFlow, cloneNode(node.origin), cloneNode(node.to))
//                override fun visitStaticCall(node: StaticCall) = StaticCall(node.function)(cloneNode(node.lastLocationAccess), *node.callArgs.map { cloneNode(it) }.toTypedArray())
//            }
//
//            fun cloneNodeWithValueDependencies(node: Node): Node = nodeCloner.cloneNode(node)
//
//            fun cloneBlock(block: Block): Block {
//                val blockClone = cloneNodeWithValueDependencies(block) as Block
//                var lastControl: Controlling = blockClone
//                for (node in block.spine) {
//                    val clone = cloneNodeWithValueDependencies(node) as Controlled
//                    lastControl.nextControl = clone
//                    if (clone is Controlling) {
//                        lastControl = clone
//                    }
//                }
//                return blockClone
//            }
//
//            for (block in calleeCompilation.session.allNodes<Block>()) {
//                cloneBlock(block)
//            }
//
//            for (block in calleeCompilation.session.allNodes<Block>().toList()) {
//                val blockClone = cloneBlock(block)
//                for (enter in block.enters) {
//                    val enterClone = cloneNodeWithValueDependencies(enter)
//                    when (enter) {
//                        is Goto -> (enterClone as Goto).exit = blockClone
//                        is If -> if (enter.trueExit == block) {
//                            (enterClone as If).trueExit = blockClone // FIXME it's extremely unreliable!!!!
//                        } else {
//                            (enterClone as If).falseExit = blockClone
//                        }
//                        else -> shouldNotReachHere(enter)
//                    }
//                }
//            }
//
//            val resultBlock = Block()
//            resultBlock.nextControl = call.nextControl
//
//            val (gotos, results) = calleeCompilation.session.allNodes<Return>().map {
//                val goto = cloneNodeWithValueDependencies(it) as Goto
//                val resultClone = cloneNodeWithValueDependencies(it.value)
//                goto to resultClone
//            }.unzip()
//
//            for (goto in gotos) {
//                goto.exit = resultBlock
//            }
//
//            val gotoInCallee = Goto()
//            call.prevControl!!.nextControl = gotoInCallee
//            gotoInCallee.exit = cloneBlock(calleeCompilation.session.entryBlock)
//
//            call.replaceValueUses(Phi(resultBlock, *results.toTypedArray()))
//            call.deregister() // FIXME?
//        }
//    }
}