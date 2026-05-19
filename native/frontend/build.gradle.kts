plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:container"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:ir.objcinterop"))
    compileOnly(intellijCore())
    api(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
