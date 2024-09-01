plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:native.tests:swiftExportTest",
        ":native:swift:swift-export-standalone:test",
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-printer:test",
    )
}