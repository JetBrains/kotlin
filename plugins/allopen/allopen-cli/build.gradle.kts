
description = "Kotlin AllOpen Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    val compileOnly by configurations
    val runtime by configurations
    compileOnly(ideaSdkCoreDeps("intellij-core"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    runtime(project(":kotlin-compiler", configuration = "runtimeJar"))
    runtime(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist {
    from(jar)
    rename("^kotlin-", "")
}

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

