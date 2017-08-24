
description = "Kotlin AllOpen Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    compileOnly(ideaSdkCoreDeps("intellij-core"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    runtime(projectDist(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

