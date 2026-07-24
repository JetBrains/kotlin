plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-assignment-compiler-plugin.cli",
        ":kotlin-assignment-compiler-plugin.common",
        ":kotlin-assignment-compiler-plugin.k2"
    )
)
