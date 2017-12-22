
apply {
    plugin("kotlin")
    plugin("java")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("util", "guava-21.0", "openapi", "idea", "asm-all") }
    compileOnly(intellijPluginDep("android")) {
        includeJars("android", "android-common", "sdklib", "sdk-common", "sdk-tools",
                    "repository", "lombok-ast-0.2.3", "layoutlib-api", "kxml2-2.3.0")
    }
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

