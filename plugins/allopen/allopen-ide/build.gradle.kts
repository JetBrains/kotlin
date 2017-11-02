
description = "Kotlin AllOpen IDEA Plugin"

apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setPlugins("gradle", "maven")
}

dependencies {
    compile(project(":kotlin-allopen-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
}

afterEvaluate {
    dependencies {
        compile(intellij { include("openapi.jar", "idea.jar") })
        compile(intellijPlugin("maven") { include("maven.jar") })
        compile(intellijPlugin("gradle") { include("gradle-tooling-api-*.jar", "gradle.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

