plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(
    listOf(
        ":kotlin-sam-with-receiver-compiler-plugin.cli",
        ":kotlin-sam-with-receiver-compiler-plugin.common",
        ":kotlin-sam-with-receiver-compiler-plugin.k1",
        ":kotlin-sam-with-receiver-compiler-plugin.k2",
    )
)
