plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":plugins:parcelize:parcelize-compiler:parcelize.backend",
        ":plugins:parcelize:parcelize-compiler:parcelize.cli",
        ":plugins:parcelize:parcelize-compiler:parcelize.common",
        ":plugins:parcelize:parcelize-compiler:parcelize.k1",
        ":plugins:parcelize:parcelize-compiler:parcelize.k2",
        ":plugins:parcelize:parcelize-runtime"
    )
)
