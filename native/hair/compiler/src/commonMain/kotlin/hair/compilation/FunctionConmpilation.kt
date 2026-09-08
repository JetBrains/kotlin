package hair.compilation

import hair.ir.*
import hair.sym.*

class FunctionCompilation(val moduleCompilation: Compilation, val function: HairFunction) {
    val session = Session()

    // TODO analysis data?

    // TODO current compilation stage?

    fun dumpHair(title: String) = moduleCompilation.config.hairDumper?.dump(this, title)

    fun dumpHairRaw(title: String, contents: String) =
        moduleCompilation.config.hairDumper?.dumpSimple(this, title, contents)
}
