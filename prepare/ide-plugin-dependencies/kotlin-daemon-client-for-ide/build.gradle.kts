plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":daemon-common",
        ":kotlin-daemon-client",
        ":kotlin-compiler-runner-unshaded",
    )
)
