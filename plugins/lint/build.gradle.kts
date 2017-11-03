
apply {
    plugin("kotlin")
    plugin("java")
}

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
    setPlugins("android")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":plugins:uast-kotlin"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellij { include("util.jar", "guava-*.jar") })
        compile(intellijPlugin("android") {
            include("android.jar", "android-common.jar", "sdklib.jar", "sdk-common.jar", "sdk-tools.jar",
                    "repository.jar", "lombok-ast-*.jar")
        })
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

