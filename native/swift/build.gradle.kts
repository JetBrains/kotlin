plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:swift:swift-export-standalone:test",
        ":native:swift:swift-export-ide:test",
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-printer:test",
        ":native:swift:swift-export-embeddable:testSwiftExportStandaloneWithEmbeddable",
    )
}