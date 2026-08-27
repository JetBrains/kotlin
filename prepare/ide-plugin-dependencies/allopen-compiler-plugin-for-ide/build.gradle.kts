plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-allopen-compiler-plugin.cli",
        ":kotlin-allopen-compiler-plugin.common",
        ":kotlin-allopen-compiler-plugin.k2",
    )
)
