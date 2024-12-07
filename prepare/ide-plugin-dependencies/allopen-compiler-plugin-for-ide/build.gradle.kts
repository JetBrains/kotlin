plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-allopen-compiler-plugin.cli",
        ":kotlin-allopen-compiler-plugin.common",
        ":kotlin-allopen-compiler-plugin.k1",
        ":kotlin-allopen-compiler-plugin.k2",
    )
)
