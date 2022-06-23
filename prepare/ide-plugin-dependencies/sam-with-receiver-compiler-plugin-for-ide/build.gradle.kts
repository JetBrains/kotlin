plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        "::kotlin-sam-with-receiver-compiler-plugin.common",
        "::kotlin-sam-with-receiver-compiler-plugin.k1"
    )
)
