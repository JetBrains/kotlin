
description = "Kotlin Scripting IDEA Plugin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-scripting-compiler-plugin"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

