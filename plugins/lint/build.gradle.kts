
apply {
    plugin("kotlin")
    plugin("java")
    plugin("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("util", "guava", "openapi", "idea", "asm-all", "annotations", rootProject = rootProject) }
    compileOnly(intellijPluginDep("android")) {
        includeJars("android", "android-common", "common", "sdklib", "sdk-common", "sdk-tools",
                    "repository", "lombok-ast", "layoutlib-api", "kxml2", rootProject = rootProject)
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

