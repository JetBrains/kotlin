
description = "Kotlin SamWithReceiver Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    val compileOnly by configurations
    val runtime by configurations
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
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
sourcesJar()
javadocJar()

publish()

dist {
    from(jar)
    rename("^kotlin-", "")
}

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

