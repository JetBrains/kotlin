plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(
    listOf(
        ":compiler:test-infrastructure",
        ":compiler:tests-common-new",
        ":compiler:test-infrastructure-utils",
        ":compiler:tests-compiler-utils",
        ":compiler:tests-spec"
    )
)
