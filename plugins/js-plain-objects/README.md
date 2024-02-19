# JavaScript Plain Objects compiler plugin

This directory contains the runtime and the compiler plugin of the `js-plain-objects` plugin.
The Gradle and Maven plugins are located in the `libraries/tools` directory.

> :warning: **The `js-plain-objects` compiler plugin only works with the K2 compiler.**

## Plugin overview

The `js-plain-objects` plugin helps you to create type-safe plain JavaScript objects. To create a plain JavaScript object, declare an [external interface](https://kotlinlang.org/docs/wasm-js-interop.html#external-interfaces) and annotate it with `@JsPlainObject`.
For example:
```kotlin
@JsPlainObject
external interface User {
    val name: String
    val age: Int
    val email: String?
}
```

The plugin adds a few extra declarations to create and copy the object with such a structure easily:
```kotlin
@JsPlainObject
external interface User {
    val name: String
    val age: Int
    val email: String?
}

// Created by the plugin declarations
inline operator fun User.Companion.invoke(name: String, age: Int, email: String? = NOTHING): User =
    js("({ name: name, age: age, email: email })")

inline fun User.copy(name: String = NOTHING, age: Int = NOTHING, email: String? = NOTHING): User =
    js("Object.assign({}, this, { name: name, age: age, email: email })")
```

To create an object with the defined structure, call `User` as a constructor:
```kotlin
fun main() {
    val user = User(name = "Name", age = 10)
    val copy = user.copy(age = 11, email = "some@user.com")

    println(JSON.stringify(user)) 
    // { "name": "Name", "age": 10 }
    println(JSON.stringify(copy)) 
    // { "name": "Name", "age": 11, "email": "some@user.com" }
}
```

The Kotlin code will be compiled into the following JavaScript code:
```javascript
function main() {
    var user = { name: "Name", age: 10 };
    var copy = Object.assign({}, user, { age: 11, email: "some@user.com" });

    println(JSON.stringify(user));
    // { "name": "Name", "age": 10 }
    println(JSON.stringify(copy));
    // { "name": "Name", "age": 11, "email": "some@user.com" }
}
```

Any JavaScript objects created with this approach are safer because you will have a compile-time error if you use the wrong property name or value type.

## Plugin structure

The plugin consists of the following parts:

1. `backend` — responsible for IR code generation.
2. `k2` — code resolution and diagnostics for the new K2 Kotlin compiler.
3. `cli` — extension points that allow the plugin to be loaded with `-Xplugin` Kotlin CLI compiler argument.
4. `common` — common declarations for other parts.

Tests and test data are common for all parts and located directly in this module. (See `testData` and `tests-gen` folders).

## Building and contributing

### Prerequisites

Before you begin, it is recommended to read the root `README.md` file and ensure that you have all the necessary tools installed.

> Note: You don't need JDK6 installed to work with this plugin.

### Install locally

Run `./gradlew dist install` to get a fresh version of the Kotlin compiler and the `js-plain-objects` plugin in your Maven local directory with the latest `2.x.255-SNAPSHOT` versions.

### Work with tests

Like most Kotlin project modules, tests are generated based on test data.
Tests are located in the `test-gen` folder and can be run using the green arrow in the IDE gutter or with the standard
`./gradlew :plugins:js-plain-objects:compiler-plugin:test` task.
To add a new test, add an appropriate file to the `testData` folder and then re-generate tests with `./gradlew :plugins:js-plain-objects:compiler-plugin:generateTests`.

### Contribute

Follow [Kotlin's contribution guidelines](../../docs/contributing.md).
If you want to report an issue, request a feature or ask for help, create an issue in our [issue tracker](https://youtrack.jetbrains.com/issues/KT). 
