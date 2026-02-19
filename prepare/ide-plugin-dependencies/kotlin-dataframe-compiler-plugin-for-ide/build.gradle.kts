plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-dataframe-compiler-plugin.k2",
        ":kotlin-dataframe-compiler-plugin.backend",
        ":kotlin-dataframe-compiler-plugin.common",
        ":kotlin-dataframe-compiler-plugin.cli",
    )
)
