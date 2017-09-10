
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

dependencies {
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

evaluationDependsOn(":kotlin-android-extensions-runtime")

runtimeJar {
    from(project(":kotlin-android-extensions-runtime").the<JavaPluginConvention>().sourceSets["main"].output.classesDirs)
}

dist()

ideaPlugin()
