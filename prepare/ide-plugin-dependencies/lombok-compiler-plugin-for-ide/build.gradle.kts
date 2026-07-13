plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-lombok-compiler-plugin.cli",
        ":kotlin-lombok-compiler-plugin.k2",
    )
)
