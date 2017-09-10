
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

val packIntoJar by configurations.creating

dependencies {
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    packIntoJar(projectClasses(":kotlin-android-extensions-runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    from(packIntoJar)
}

dist()

ideaPlugin()
