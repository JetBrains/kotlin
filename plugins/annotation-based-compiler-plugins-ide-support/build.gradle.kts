
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
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) { includeJars("maven", "maven-server-api") } }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-tooling-api", "gradle", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("openapi", "idea", "extensions", "jdom", "util") }
 }

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

