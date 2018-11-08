plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:resolution"))
    compile(project(":compiler:plugin-api"))
    compile(project(":idea"))
    compile(intellijCoreDep()) { includeJars("intellij-core", "jdom") }
    compileOnly(intellijDep()) { includeJars("annotations", "trove4j", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

ideaPlugin {
    from(jar)
}