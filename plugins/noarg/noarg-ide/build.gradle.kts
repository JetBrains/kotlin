
description = "Kotlin NoArg IDEA Plugin"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-noarg-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep()) { includeJars("openapi", "idea", "util", "extensions") }
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) { includeJars("maven") } }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-api", "gradle", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()
