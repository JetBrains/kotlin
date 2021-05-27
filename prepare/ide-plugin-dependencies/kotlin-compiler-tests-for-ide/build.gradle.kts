plugins {
    kotlin("jvm")
}

publishJarsForIde(listOf(":compiler:test-infrastructure", ":compiler:tests-common-new", ":compiler:test-infrastructure-utils"))
