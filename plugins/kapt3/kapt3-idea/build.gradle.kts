
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":kotlin-annotation-processing"))
    compile(ideaSdkDeps("openapi", "external-system-rt", "platform-api"))
    compile(ideaPluginDeps("gradle-api", "gradle", plugin = "gradle"))
    compile(ideaPluginDeps("android", "android-common", "sdklib", "sdk-common", "sdk-tools", plugin = "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar()

ideaPlugin {
    from(jar)
}
