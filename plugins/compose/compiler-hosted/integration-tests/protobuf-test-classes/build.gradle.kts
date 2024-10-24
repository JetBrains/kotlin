plugins {
    id("com.google.protobuf") version "0.9.4"
    id("java")
}

dependencies {
    implementation("com.google.protobuf:protobuf-javalite:4.28.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }

    generateProtoTasks.all().configureEach {
        builtins {
            val java by getting {
                option("lite")
            }
        }
    }
}
