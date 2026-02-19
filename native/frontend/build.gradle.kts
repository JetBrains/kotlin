plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":core:compiler.common.native"))
    compileOnly(project(":compiler:ir.objcinterop"))
    compileOnly(intellijCore())
    api(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
