class SplitSchemeProbe {
    val label: String = "probe"

    fun describe(): String = "SplitSchemeProbe($label)"

    private fun internalHelper(): Int = label.length

    fun compute(): Int = internalHelper() * 2
}

fun topLevelProbeEntry(): String = SplitSchemeProbe().describe()
