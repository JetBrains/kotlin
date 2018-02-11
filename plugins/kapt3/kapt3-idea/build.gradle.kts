
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":kotlin-annotation-processing"))
    compileOnly(intellijDep()) { includeJars("openapi", "external-system-rt", "util") }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-core", "gradle-tooling-api", "gradle", rootProject = rootProject) }
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
