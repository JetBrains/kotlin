plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-dataframe-compiler-plugin.k2",
        ":kotlin-dataframe-compiler-plugin.backend",
        ":kotlin-dataframe-compiler-plugin.common",
        ":kotlin-dataframe-compiler-plugin.cli",
    )
)
