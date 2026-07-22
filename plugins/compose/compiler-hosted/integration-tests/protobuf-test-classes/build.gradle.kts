plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("com.google.protobuf") version "0.9.6"
    id("java")
}

dependencies {
    implementation(libs.protobuf.java.lite)
}

protobuf {
    protoc {
        val protocVersion = libs.versions.protobuf.get()
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }

    generateProtoTasks.all().configureEach {
        builtins {
            val java by getting {
                option("lite")
            }
        }
    }
}

registerInAggregateGenerateSources("generateProto")
