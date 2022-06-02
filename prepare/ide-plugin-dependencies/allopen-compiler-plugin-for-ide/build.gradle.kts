plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-allopen-compiler-plugin.common",
        ":kotlin-allopen-compiler-plugin.k1",
    )
)
