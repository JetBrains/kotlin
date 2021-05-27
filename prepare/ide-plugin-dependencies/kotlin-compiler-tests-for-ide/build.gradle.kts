plugins {
    kotlin("jvm")
}

publishTestJarsForIde(listOf(":compiler:test-infrastructure", ":compiler:tests-common-new", ":compiler:test-infrastructure-utils"))
