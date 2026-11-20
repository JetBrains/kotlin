# kotlin-import-models

The contract between the Kotlin Gradle Plugin and an IDE for the new Kotlin import:
the Protobuf schema of the import models and its JVM bindings, published as `org.jetbrains.kotlin:kotlin-import-models`.

The IDE requests a model through one Gradle Tooling API model builder with a `ModelRequest` (a model id from `KotlinImportModelIds`
and serialized parameters) and receives a `KotlinGradleModel` that holds a serialized `Result`.

## Public API

The public API is the `org.jetbrains.kotlin.importmodels` package and its `proto` subpackage, which holds the classes generated
from [kotlin_import_models.proto](src/main/proto/kotlin_import_models.proto).
The bundled Protobuf runtime is relocated to `org.jetbrains.kotlin.importmodels.internal.protobuf`.
It is an implementation detail that can change in any release, so use it only through the generated classes.

## Schema rules

Until the Kotlin 2.5.20 release, the schema may change incompatibly.
From that release on, changes are additive: a published field number or enum value keeps its meaning, and a removed field is `reserved`.
Enums are open, with a zero `*_UNSPECIFIED` value and a value prefix unique to the enum.
Consumers decide compatibility by `BaseModel.plugin_version`.

The schema has no `map<>` fields.
The producer sorts set-like repeated fields and keeps the order of order-significant ones, such as classpath entries.
Nothing relies on byte equality of serialized messages, because Protobuf serialization is not canonical across versions.

## Changing the schema

The generated sources are checked in. After changing the `.proto` file, run `./gradlew :kotlin-import-models:protoc` and commit the result.
After a change to the public API, run `./gradlew :kotlin-import-models:apiDump`.
