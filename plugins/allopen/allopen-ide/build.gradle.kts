
description = "Kotlin AllOpen IDEA Plugin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-allopen-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compile(ideaSdkDeps("openapi", "idea"))
    //compile(ideaPluginDeps("maven", plugin = "maven"))
    compile(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

