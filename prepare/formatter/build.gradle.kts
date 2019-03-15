
description = "Kotlin Formatter"

plugins {
    java
}

runtimeJar {
    archiveName = "kotlin-formatter.jar"
    dependsOn(":idea:formatter:classes")
    project(":idea:formatter").let { p ->
        p.pluginManager.withPlugin("java") {
            from(p.mainSourceSet.output)
        }
    }
    from(fileTree("$rootDir/idea/formatter")) { include("src/**") } // Eclipse formatter sources navigation depends on this
}

sourceSets {
    "main" {}
    "test" {}
}

