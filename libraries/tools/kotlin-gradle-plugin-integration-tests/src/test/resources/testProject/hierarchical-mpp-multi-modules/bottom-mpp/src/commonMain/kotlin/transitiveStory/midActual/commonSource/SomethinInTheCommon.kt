package transitiveStory.midActual.commonSource

val moduleName = "bottom-mpp"
expect val sourceSetName: String

expect open class SomeMPPInTheCommon() {
    val simpleVal: Int

    companion object Compainon {
        val inTheCompanionOfBottomActualDeclarations: String
    }
}

fun regularTLfunInTheMidActualCommmon(s: String): String {
    return "I'm a function at the top level of a file in `commonMain` source set of module $moduleName." +
            "This is the message I've got: \n`$s`"
}
