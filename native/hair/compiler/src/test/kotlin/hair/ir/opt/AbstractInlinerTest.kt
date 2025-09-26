package hair.ir.opt

import hair.compilation.Compilation
import hair.compilation.FunctionCompilation
import hair.ir.InitialMemoryBuilder
import hair.ir.IrTest
import hair.ir.buildInitialIR
import hair.sym.HairFunction

interface AbstractInlinerTest : IrTest {

    class InlinerTestCompilation(val compilation: Compilation) {
        fun define(function: HairFunction, buildBody: InitialMemoryBuilder.() -> Unit): FunctionCompilation {
            val funCompilation = compilation.getCompilation(function)
            funCompilation.session.apply {
                buildInitialIR {
                    buildBody()
                }
            }
            return funCompilation
        }
    }

    fun inlinerTest(test: InlinerTestCompilation.() -> Unit) {
        InlinerTestCompilation(Compilation()).test()
    }
}