plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-lombok-compiler-plugin.cli",
        ":kotlin-lombok-compiler-plugin.k2",
    )
)
