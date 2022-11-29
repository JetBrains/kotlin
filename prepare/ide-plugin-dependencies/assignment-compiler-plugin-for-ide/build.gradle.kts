plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-assignment-compiler-plugin.common",
        ":kotlin-assignment-compiler-plugin.k1"
    )
)
