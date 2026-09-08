plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-sam-with-receiver-compiler-plugin.cli",
        ":kotlin-sam-with-receiver-compiler-plugin.common",
        ":kotlin-sam-with-receiver-compiler-plugin.k2",
    )
)
