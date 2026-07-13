plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlinx-serialization-compiler-plugin.cli",
        ":kotlinx-serialization-compiler-plugin.common",
        ":kotlinx-serialization-compiler-plugin.k2",
        ":kotlinx-serialization-compiler-plugin.backend"
    )
)
