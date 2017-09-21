
description = "Kotlin Android Extensions Runtime"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(ideaPluginDeps("layoutlib", plugin = "android"))
    runtime(commonDep("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

publish()
