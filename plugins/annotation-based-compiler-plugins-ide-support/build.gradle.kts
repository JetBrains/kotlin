
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
    compileOnly(intellijPluginDep("maven")) { includeJars("maven.jar", "maven-server-api.jar") }
        compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-tooling-api-3.5.jar", "gradle.jar") }
        compileOnly(intellijDep()) { includeJars("openapi.jar", "idea.jar", "extensions.jar", "jdom.jar", "util.jar") }
    }

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

