package hair.opt

import hair.compilation.Compilation
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.shouldNotReachHere

// FIXME AnyCall
fun Compilation.inline(call: InvokeStatic) {
    val callerSession = call.session // FIXME obtain compilation by session?
    val calleeCompilation = getCompilation(call.function)

    with (callerSession) {
        modifyIR {
            val toClone = calleeCompilation.session.allNodes().filterNot { it is Return }
            val clones = callerSession.cloneNodes(toClone) {
                when (it) {
                    is Param -> call.callArgs[it.index]
                    calleeCompilation.session.entry -> BlockEntry(Goto(call.control))
                    else -> null
                }
            }

            val (returns, returnedValues) = calleeCompilation.session.allNodes<Return>().map {
                val control = clones[it.control]!! as Controlling
                val value = it.resultOrNull?.let { clones[it]!! }
                Goto(control) to value
            }.unzip()

            val resultBlock = BlockEntry(*returns.toTypedArray())
            call.next.control = resultBlock
            call.replaceValueUsesAndKill(Phi(resultBlock, *returnedValues.toTypedArray()))
        }
    }
}