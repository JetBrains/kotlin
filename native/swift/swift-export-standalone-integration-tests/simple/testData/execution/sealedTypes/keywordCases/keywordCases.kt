// KIND: STANDALONE
// MODULE: SealedKeywordCases
// FILE: main.kt

// Inheritor names that become Swift keywords after camelCasing (`default` is also the `switch`
// catch-all): the cases must be backticked and stay matchable. In a package on purpose, so the
// classes land under `ExportedKotlinPackages`.

package keyword.host

sealed interface KeywordHost

class Default : KeywordHost {
    override fun toString(): String = "Default"
}

class Case : KeywordHost {
    override fun toString(): String = "Case"
}

class Init : KeywordHost {
    override fun toString(): String = "Init"
}

class Self : KeywordHost {
    override fun toString(): String = "Self"
}

fun createDefault(): KeywordHost = Default()

fun createCase(): KeywordHost = Case()

fun createInit(): KeywordHost = Init()

fun createSelf(): KeywordHost = Self()
