package transitiveStory.bottomActual.mppBeginning

val moduleName = "top-mpp"
val commonInt = 42
expect val sourceSetName: String

expect open class BottomActualDeclarations() {
    val simpleVal: Int

    companion object Compainon {
        val inTheCompanionOfBottomActualDeclarations: String
    }
}

fun regularTLfunInTheBottomActualCommmon(s: String): String {
    return "I'm a function at the top level of a file in `commonMain` source set of module $moduleName." +
            "This is the message I've got: \n`$s`"
}

// shouldn't be resolved
/*
fun bottActualApiCaller(k: KotlinApiContainer, s: JavaApiContainer) {
    // val first = privateKotlinDeclaration
}*/

internal val tlInternalInCommon = 42

// has a child in jsJvm18Main
open class Outer {
    private val a = 1
    protected open val b = 2
    internal val c = 3
    val d = 4  // public by default

    protected class Nested {
        public val e: Int = 5
    }
}

// has a child in jsJvm18Main
expect open class MPOuter {
    protected open val b: Int
    internal val c: Int
    val d: Int // public by default

    protected class MPNested {
        public val e: Int
    }
}

