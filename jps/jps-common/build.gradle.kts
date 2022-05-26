
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":js:js.frontend"))
    implementation(project(":kotlin-reflect"))
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
