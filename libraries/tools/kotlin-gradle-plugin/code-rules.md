# Gradle task class must be well formatted

Applies to:
```
**/src/**/*.kt
```

Rules for gradle task classes:
- it must be abstract
- it must extend `org.gradle.api.DefaultTask`
- it should have `Task` postfix.
- all internal and public properties should be marked with Gradle's annotations: `@get:Internal`, `@get:Input`, `@get:InputFiles`, `@get:ClassPath`, `@get:Output`
- task body should use all properties with `@get:Internal`, exceptions possible when internal property is supposed to be used outside task.
- all input/output properties must use Gradle Provider API (Provider, Property, FileCollection, ListProperty, SetProperty, DirectoryProperty, DirectoryTreeProperty, etc)

Exceptions to rules:
- for 'should' rules it is allowed to suppress them with explanation comment

# Task registration factory method should be in the same file with task

Applies to:
```
**/src/**/*.kt
```

Registration function of Gradle Tasks (when they extend `org.gradle.api.DefaultTask()`) in Kotlin Gradle Plugin
should be placed in the same file with task.

With few additional requirements:
- it should use `locateOrRegisterTask` method, that provides idempotency.
- it should start with `locateOrRegister` prefix. And postfix should match the task name. But `kotlin` prefix can be omitted.
