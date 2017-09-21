
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":kotlin-annotation-processing"))
    compile(ideaSdkDeps("openapi", "external-system-rt"))
    compile(ideaPluginDeps("gradle-core", "gradle-tooling-api", "gradle", plugin = "gradle"))
    compile(ideaPluginDeps("android", plugin = "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar()

ideaPlugin {
    from(jar)
}
