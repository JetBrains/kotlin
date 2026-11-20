plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCore())
    api(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

standardPublicJars()
