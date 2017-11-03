
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setPlugins("gradle", "maven")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijPlugin("maven") { include("maven.jar", "maven-server-api.jar") })
        compileOnly(intellijPlugin("gradle") { include("gradle-tooling-api-*.jar", "gradle.jar") })
        compileOnly(intellij { include("openapi.jar", "idea.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

