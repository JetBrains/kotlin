package hair.opt

import hair.compilation.Compilation
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.shouldNotReachHere
import hair.utils.toTypedArray

// FIXME AnyCall
fun Compilation.inline(call: InvokeStatic) {
    val callerSession = call.session // FIXME obtain compilation by session?
    val calleeCompilation = getCompilation(call.function)

    with (callerSession) {
        modifyControlFlow {
            val toClone = calleeCompilation.session.allNodes().filterNot { it is Return }
            val clones = callerSession.cloneNodes(toClone) {
                when (it) {
                    // FIXME make Unreachable value-numbered
                    is Unreachable -> unreachable
                    is Param -> call.callArgs[it.index]
                    calleeCompilation.session.entry -> BlockEntry(Goto(call.control))
                    else -> null
                }
            }

            val returns = calleeCompilation.session.allNodes<Return>().map {
                val control = clones[it.control]!! as Controlling
                val value = it.resultOrNull?.let { clones[it]!! }
                Goto(control) to value
            }.toList()

            val exits = returns.map { it.first }.toTypedArray()
            val resultBlock = BlockEntry(*exits)
            val result = if (returns.any { it.second != null }) {
                Phi(
                    resultBlock,
                    *returns.map { ValueAndExit(it.second!!, it.first) }.toTypedArray()
                )
            } else null
            call.replaceWith(resultBlock, result)
        }
    }
}