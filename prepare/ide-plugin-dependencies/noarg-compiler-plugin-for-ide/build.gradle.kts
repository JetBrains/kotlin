plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-noarg-compiler-plugin.common",
        ":kotlin-noarg-compiler-plugin.k1",
        ":kotlin-noarg-compiler-plugin.backend"
    )
)
