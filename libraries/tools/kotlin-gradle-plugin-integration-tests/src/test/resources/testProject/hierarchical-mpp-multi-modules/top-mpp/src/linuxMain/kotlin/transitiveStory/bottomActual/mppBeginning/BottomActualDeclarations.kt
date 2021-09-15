package transitiveStory.bottomActual.mppBeginning

actual open class BottomActualDeclarations {
    actual val simpleVal: Int = commonInt

    actual companion object Compainon {
        actual val inTheCompanionOfBottomActualDeclarations: String =
            "I'm a string from the companion object of `$this` in `$sourceSetName` module `$moduleName`"
    }
}

actual val sourceSetName: String = "linuxMain"