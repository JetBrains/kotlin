plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-noarg-compiler-plugin.cli",
        ":kotlin-noarg-compiler-plugin.common",
        ":kotlin-noarg-compiler-plugin.k2",
        ":kotlin-noarg-compiler-plugin.backend"
    )
)
