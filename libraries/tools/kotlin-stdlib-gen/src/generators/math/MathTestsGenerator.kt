package generators.math

import templates.capitalize
import java.io.File
import java.io.FileWriter
import kotlin.reflect.KCallable

internal class MathTestsGenerator(
    private val outputFile: File,
    private val testPoints: List<Double>,
    private val functions: List<Model>
) {
    sealed class Model(val exact: Boolean) {
        abstract val function: Function<*>
    }
    class ModelFunction1(override val function: Function1<Double, Double>, exact: Boolean = true, val customTestPoint: Double? = null) : Model(exact)
    class ModelFunction2(override val function: Function2<Double, Double, Double>, exact: Boolean = true) : Model(exact)

    private val doubleSpecialPoints = listOf(
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.MIN_VALUE,
        Double.MAX_VALUE,
        Double.NaN,
        123456.789123e200, //Just a random point
    )

    private fun mutatePoints(points: List<Double>): Sequence<Double> = sequence {
        for (pt in points) {
            yield(pt)
            yield(pt + 0.1e-10)
            yield(pt - 0.1e-10)
            yield(pt * 0.5)
            yield(pt / 0.5)
            yield(pt * 0.25)
            yield(pt / 0.25)
            yield(-pt)
            yield(-pt + 0.1e-10)
            yield(-pt - 0.1e-10)
            yield(-pt * 0.5)
            yield(-pt / 0.5)
            yield(-pt * 0.25)
            yield(-pt / 0.25)
        }
    }

    private fun generatePoints(): Sequence<Double> =
        doubleSpecialPoints.asSequence() + mutatePoints(testPoints)

    private fun generate2dPoints(): Sequence<Pair<Double, Double>> =
        generatePoints().flatMap { lhsElem -> generatePoints().map { rhsElem -> lhsElem to rhsElem } }

    private fun Double.toULongString() = "0x${toBits().toULong().toString(16)}UL"

    private fun Sequence<Double>.toULongVariableList(name: String): String = buildString {
        appendLine("val $name = arrayOf(")
        this@toULongVariableList.forEachIndexed { i, d ->
            append("${d.toULongString()}, ".prependIndent())
            if (i % 4 == 3) appendLine()
        }
        appendLine()
        appendLine(")")
    }

    private fun generateTestMethods(): String = buildString {
        functions.forEach { model ->
            appendLine()
            appendLine("@Test")
            val function = model.function
            val name = (function as KCallable<*>).name
            appendLine("fun test${name.capitalize()}() {")
            val answers = when (model) {
                is ModelFunction1 -> generatePoints().map { arg -> model.function(arg) }
                is ModelFunction2 -> generate2dPoints().map { arg -> model.function(arg.first, arg.second) }
            }
            appendLineWithIndent(answers.toULongVariableList("answers"))
            appendLine()
            appendLineWithIndent("checkAnswers(::$name, arguments, answers, ${model.exact})")
            val specialFunctionPoint = (model as? ModelFunction1)?.customTestPoint
            if (specialFunctionPoint != null) {
                appendLineWithIndent(mutatePoints(listOf(specialFunctionPoint)).toULongVariableList("specialFunctionPointArguments"))
                appendLineWithIndent(
                    mutatePoints(listOf(specialFunctionPoint)).map(model.function).toULongVariableList("specialFunctionPointResults")
                )
                appendLineWithIndent("checkAnswers(::$name, specialFunctionPointArguments, specialFunctionPointResults, ${model.exact})")
            }
            appendLine("}")
        }
    }


    private fun StringBuilder.appendLineWithIndent(string: String) {
        appendLine(string.prependIndent())
    }

    private fun FileWriter.writeTestClass() {
        appendLine(
            """
class ${outputFile.nameWithoutExtension.capitalize()}Test {
${("private " + generatePoints().toULongVariableList("arguments")).prependIndent()}
${generateTestMethods().prependIndent()}
}
        """
        )
    }

    fun generate() {
        FileWriter(outputFile).use { writer ->
            writer.writeHeader(outputFile)
            writer.appendLine()
            writer.writeTestClass()
        }
    }
}