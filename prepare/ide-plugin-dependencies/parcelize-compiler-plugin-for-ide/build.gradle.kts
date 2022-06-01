plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":plugins:parcelize:parcelize-compiler:parcelize.common",
        ":plugins:parcelize:parcelize-compiler:parcelize.k1",
        ":plugins:parcelize:parcelize-compiler:parcelize.backend",
        ":plugins:parcelize:parcelize-runtime"
    )
)
