plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli",
        ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common",
        ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2",
        ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend",
    )
)
