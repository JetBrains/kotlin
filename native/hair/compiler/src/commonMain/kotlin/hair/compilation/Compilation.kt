package hair.compilation

import hair.sym.*

class Compilation {
    private val funCompilations = mutableMapOf<HairFunction, FunctionCompilation>()

    fun getCompilation(function: HairFunction) = funCompilations.getOrPut(function) {
        FunctionCompilation(this, function)
    }
}