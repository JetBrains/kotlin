
description = "Kotlin Android Extensions Runtime"

apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(ideaPluginDeps("layoutlib", plugin = "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

publish()
