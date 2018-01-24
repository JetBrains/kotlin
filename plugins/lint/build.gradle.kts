
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
    compileOnly(intellijDep()) { includeJars("util", "guava", "openapi", "idea", "asm-all", rootProject = rootProject) }
    compileOnly(intellijPluginDep("android")) {
        includeJars("android", "android-common", "sdklib", "sdk-common", "sdk-tools",
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

