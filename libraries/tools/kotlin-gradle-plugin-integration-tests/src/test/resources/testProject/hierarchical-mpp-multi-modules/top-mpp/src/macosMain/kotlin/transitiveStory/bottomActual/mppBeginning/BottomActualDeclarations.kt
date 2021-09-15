package transitiveStory.bottomActual.mppBeginning

actual open class BottomActualDeclarations {
    actual val simpleVal: Int = commonInt

    actual companion object Compainon {
        actual val inTheCompanionOfBottomActualDeclarations: String =
                "I'm a string from the companion object of `$this` in `$sourceSetName` module `$moduleName`"
    }
}

actual open class MPOuter {
    protected actual open val b: Int = 4325
    internal actual val c: Int = 2345
    actual val d: Int = 325

    protected actual class MPNested {
        actual val e: Int = 345
    }

}

class ChildOfCommonInMacos : Outer() {
    override val b: Int
        get() = super.b + 243
    val callAlso = super.c // internal in Outer

    private val other = Nested()
}

class ChildOfMPOuterInMacos : MPOuter() {
    private val sav = MPNested()
}

actual val sourceSetName: String = "macosMain"