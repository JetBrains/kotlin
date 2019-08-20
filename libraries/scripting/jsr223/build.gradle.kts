
plugins {
    maven
    kotlin("jvm")
}

jvmTarget = "1.6"

val publishedRuntime by configurations.creating {
    the<MavenPluginConvention>()
        .conf2ScopeMappings
        .addMapping(0, this, Conf2ScopeMappingContainer.RUNTIME)
}

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

standardPublicJars()

projectTest(parallel = true)

