
description = "Kotlin IDE Lazy Resolver"

apply { plugin("java") }

runtimeJar {
    archiveName = "kotlin-ide-common.jar"
    dependsOn(":idea:ide-common:classes")
    project(":idea:ide-common").let { p ->
        p.pluginManager.withPlugin("java") {
            from(p.the<JavaPluginConvention>().sourceSets.getByName("main").output)
        }
    }
    from(fileTree("$rootDir/idea/ide-common")) { include("src/**") } // Eclipse formatter sources navigation depends on this
}

sourceSets {
    "main" {}
    "test" {}
}

