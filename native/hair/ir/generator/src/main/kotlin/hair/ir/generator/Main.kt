package hair.ir.generator

import hair.ir.generator.toolbox.Generator
import java.io.File

fun main(args: Array<String>) {
    val generationPath = File(args.first())
    val generator = Generator(generationPath)

    generator.generate(Utils)
    generator.generate(ControlFlow)
    generator.generate(DataFlow)
    generator.generate(Arithmetics)
    generator.generate(Object)
    generator.generate(Calls)

    generator.generateSession()
    generator.generateVisitor()
    generator.generateBuilder()
    generator.generateCloner()
}