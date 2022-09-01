plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-value-container-assignment-compiler-plugin.common",
        ":kotlin-value-container-assignment-compiler-plugin.k1"
    )
)
