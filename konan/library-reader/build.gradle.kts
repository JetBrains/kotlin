plugins {
    maven
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:cli-common"))

    compile(project(":kotlin-native:kotlin-native-utils"))
    compile(project(":kotlin-util-io"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-util-klib-metadata"))

    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

standardPublicJars()
