
description = "Kotlin NoArg Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:util"))
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

dist {
    from(jar)
    rename("^kotlin-", "")
}

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

