
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    rootProject.extra["compilerModulesForJps"]
        .let { it as List<String> }
        .forEach { implementation(project(it)) }

    compileOnly(intellijUtilRt())
    compileOnly(intellijPlatformUtil())
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsModelSerialization())

    testImplementation(project(":compiler:cli-common"))
    testImplementation(jpsModelSerialization())
    testImplementation(project(":kotlin-reflect"))
    testImplementation(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
