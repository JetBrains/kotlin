
description = "Kotlin Serialization Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    compileOnly(ideaSdkCoreDeps("intellij-core"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))

    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))
//    val compileOnly by configurations
//    val runtime by configurations
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

val serialPluginDir: File by rootProject.extra
dist(targetDir = File(serialPluginDir,"lib"), targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}
