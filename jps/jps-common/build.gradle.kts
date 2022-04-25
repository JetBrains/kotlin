
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))
    testImplementation(project(":kotlin-reflect"))
    api(project(":compiler:util"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:frontend.java"))
    api(project(":js:js.frontend"))
    api(project(":native:frontend.native"))
    compileOnly(intellijUtilRt())
    compileOnly(intellijPlatformUtil())
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsModelSerialization())

    testApi(jpsModelSerialization())
    testApi(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
