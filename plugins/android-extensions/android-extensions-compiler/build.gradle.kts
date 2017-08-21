
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

dependencies {
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(ideaPluginDeps("layoutlib", plugin = "android"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../android-extensions-runtime/src")
    }
    "test" {}
}

runtimeJar ()

dist()

ideaPlugin()

