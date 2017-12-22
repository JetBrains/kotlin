
description = "Kotlin SamWithReceiver IDEA Plugin"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(intellijDep()) { includeJars("openapi", "extensions", "util") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

