package komem.litmus

import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

fun generateWrapperFile(test: LitmusTest<*>, jcstressDirectory: Path): Boolean {
    val targetFile = jcstressDirectory / "generatedSrc/main/java/komem/litmus/${test.javaClassName}.java"
    targetFile.createParentDirectories()
    val targetCode = try {
        generateWrapperCode(test)
    } catch (e: Throwable) {
        System.err.println("WARNING: could not generate wrapper for ${test.name} because: ${e.message}")
        return false
    }
    targetFile.writeText(targetCode)
    return true
}

private fun generateWrapperCode(test: LitmusTest<*>): String {
    val stateClass = test.stateProducer()::class
    require(stateClass.allSuperclasses.contains(LitmusAutoOutcome::class)) {
        "to use JCStress, test state must extend some LitmusAutoOutcome (e.g. LitmusIIOutcome)"
    }

    val autoOutcomeClassList = stateClass.superclasses.filter { it.isSubclassOf(LitmusAutoOutcome::class) }
    require(autoOutcomeClassList.size == 1) { "test state should extend exactly one LitmusAutoOutcome" }
    val outcomeTypeName = autoOutcomeClassList.first().simpleName!!
        .removePrefix("Litmus")
        .removeSuffix("Outcome")
    val (outcomeVarType, outcomeVarCount) = when (outcomeTypeName) {
        "I" -> "Integer" to 1
        "II" -> "Integer" to 2
        "III" -> "Integer" to 3
        "IIII" -> "Integer" to 4
        else -> error("unknown AutoOutcome type $outcomeTypeName")
    }

    val javaTestGetter: String = run {
        val parts = test.name.split(".")
        val getter = "get" + parts.last() + "()"
        val className = parts.dropLast(1).last() + "Kt"
        val packages = parts.dropLast(2)
        val packagesLine = if (packages.isEmpty()) "" else packages.joinToString(".", postfix = ".")
        "$packagesLine$className.$getter"
    }

    val javaArbiterDecl: String = run {
        val jcstressResultClassName = outcomeTypeName + "_Result"
        if (outcomeVarCount > 1) {
            """
@Arbiter
public void a($jcstressResultClassName r) {
    List<$outcomeVarType> result = (List<$outcomeVarType>) fA.invoke(state);
    ${List(outcomeVarCount) { "r.r${it + 1} = result.get($it);" }.joinToString("\n    ")}
}
            """.trim()
        } else {
            // single values are handled differently
            """
            @Arbiter
            public void a($jcstressResultClassName r) {
                r.r1 = ($outcomeVarType) fA.invoke(state);
            }
            """.trimIndent()
        }
    }

    val jcstressOutcomeDecls: String = run {
        val outcomes = test.outcomeSpec.accepted.associateWith { "ACCEPTABLE" } +
                test.outcomeSpec.interesting.associateWith { "ACCEPTABLE_INTERESTING" } +
                test.outcomeSpec.forbidden.associateWith { "FORBIDDEN" }

        // since only AutoOutcome is allowed, each outcome is a list (unless it's a single value)
        outcomes.map { (o, t) ->
            val oId = if (outcomeVarCount > 1) (o as List<*>).joinToString(", ") else o.toString()
            "@Outcome(id = \"$oId\", expect = $t)"
        }.joinToString("\n")
    }

    val jcstressDefaultOutcomeType = when (test.outcomeSpec.default) {
        LitmusOutcomeType.ACCEPTED -> "ACCEPTABLE"
        LitmusOutcomeType.FORBIDDEN -> "FORBIDDEN"
        LitmusOutcomeType.INTERESTING -> "ACCEPTABLE_INTERESTING"
    }

    return wrapperCode(test, jcstressOutcomeDecls, jcstressDefaultOutcomeType, javaTestGetter, javaArbiterDecl)
}

private fun javaThreadFunctionDecl(index: Int) =
    "private static final Function1<Object, Unit> fT$index = test.getThreadFunctions().get($index);"

private fun javaActorDecl(index: Int) = """
    @Actor
    public void t$index() {
        fT$index.invoke(state);
    }
    """.trimIndent()

fun wrapperCode(
    test: LitmusTest<*>,
    jcstressOutcomeDecls: String,
    jcstressDefaultOutcomeType: String,
    javaTestGetter: String,
    javaArbiterDecl: String,
) = """
package komem.litmus;

import komem.litmus.testsuite.*;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

import static org.openjdk.jcstress.annotations.Expect.*;

import java.util.List;

@JCStressTest
@State
$jcstressOutcomeDecls
@Outcome(expect = $jcstressDefaultOutcomeType)
public class ${test.javaClassName} {

    private static final LitmusTest<Object> test = (LitmusTest<Object>) $javaTestGetter;
    ${List(test.threadCount) { javaThreadFunctionDecl(it) }.joinToString("\n    ")}
    private static final Function1<Object, Object> fA = test.getOutcomeFinalizer();

    public ${test.javaClassName}() {}

    public Object state = test.getStateProducer().invoke();
    
    ${List(test.threadCount) { javaActorDecl(it).padded(4) }.joinToString("\n\n    ")}
    
    ${javaArbiterDecl.padded(4)}
}
""".trimIndent()

private fun String.padded(padding: Int) = replace("\n", "\n" + " ".repeat(padding))
