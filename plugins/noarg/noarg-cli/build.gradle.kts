
description = "Kotlin NoArg Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    val compileOnly by configurations
    val runtime by configurations
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    runtime(project(":kotlin-compiler", configuration = "runtimeJar"))
    runtime(project(":kotlin-stdlib"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

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

