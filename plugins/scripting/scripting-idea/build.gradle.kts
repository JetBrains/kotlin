
description = "Kotlin Scripting IDEA Plugin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-scripting-compiler"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

