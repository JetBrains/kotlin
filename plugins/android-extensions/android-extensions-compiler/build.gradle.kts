
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(ideaPluginDeps("layoutlib", plugin = "android"))
}

configureKotlinProjectSources("android-extensions-compiler/src", "android-extensions-runtime/src", sourcesBaseDir = File(rootDir, "plugins", "android-extensions"))
configureKotlinProjectResourcesDefault(sourcesBaseDir = File(rootDir, "plugins", "android-extensions", "android-extensions-compiler", "src"))
configureKotlinProjectNoTests()

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../android-extensions-runtime/src")
    }
    "test" { none() }
}

runtimeJar ()

dist()

ideaPlugin()

