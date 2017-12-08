
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":kotlin-annotation-processing"))
    compileOnly(intellijDep()) { includeJars("openapi", "external-system-rt", "util") }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-core-3.5", "gradle-tooling-api-3.5", "gradle") }
    compileOnly(intellijPluginDep("android")) { includeJars("android", "android-common", "sdklib", "sdk-common", "sdk-tools") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar()

ideaPlugin {
    from(jar)
}
