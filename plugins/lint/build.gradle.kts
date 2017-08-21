
apply {
    plugin("kotlin")
    plugin("java")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":plugins:uast-kotlin"))
    compile(ideaPluginDeps("android", "android-common", "sdk-common", "sdklib", "sdk-tools", "repository", "lombok-ast", "kxml2", plugin = "android"))
    compile(ideaSdkCoreDeps("intellij-core", "util"))
    compile(ideaSdkDeps("guava"))
}

sourceSets {
    "main" {
        java.srcDirs("android-annotations/src",
                     "lint-api/src",
                     "lint-checks/src",
                     "lint-idea/src")
    }
    "test" {}
}

