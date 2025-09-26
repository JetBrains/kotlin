package hair.opt

import hair.sym.*
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*
import hair.utils.printGraphviz

fun Session.buildEscapeGraph() {
    val writes = allNodes<WriteMemory>().filter { it.field.type is Type.Reference }.toList()
    for (write in writes) {
        modifyIR {
            val written = write.value

            val variable = written.replaceValueUsesByNewVar()

            val escapeInto = when (write) {
                is WriteField -> write.obj
                else -> Unknown()
            }
            val escape = Escape(write, written, escapeInto)
            // FIXME remove cast
            insertAfter(write as Spinal) { AssignVar(variable)(escape) }

            buildSSA()
        }
    }
}

