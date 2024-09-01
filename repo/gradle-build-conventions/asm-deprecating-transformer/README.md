# asm-deprecating-transformer

## Description

This plugin allows you to deprecate symbols bundled into a fat JAR based on specified filters. This can be particularly useful for hiding bundled dependencies that cannot be relocated for certain reasons.

* Use with caution and include migration guides in the deprecation message if possible.
* Be mindful of potential Fully Qualified Name (FQN) clashes in the classpath and issues related to classpath ordering.
* This transformer was initially designed to solve a particular problem (KT-70251) and may not fit general needs yet.

## Usage

Here is an example of how to apply this transformer to the `shadowJar` task in a project:

```kotlin
plugins {
    id("asm-deprecating-transformer")
}

// ...

tasks.named<ShadowJar>("shadowJar") {
    asmDeprecation {
        deprecateClassesByPattern(
            pattern = "org.example.**",
            deprecationMessage = "Deprecated by asm-deprecating-transformer",
            exclusions = listOf("org.example.api.**"),
        )
    }
}
```

Given an original shadow JAR with the following structure:

```
com/example/App
org/example/Tree
org/example/api/Fruit
org/example/fruit/Apple
org/example/fruit/Banana
```

These classes will be marked as deprecated:

```
com/example/App
org/example/Tree <-
org/example/api/Fruit
org/example/fruit/Apple <-
org/example/fruit/Banana <-
```

In addition to deprecating classes, you can dump a list of packages containing deprecated classes and verify it against the actually deprecated ones:

```kotlin
asmDeprecation {
    registerDumpDeprecationsTask(
        shadowJarTaskName = "shadowJar", 
        suffix = "Jar", // the task name is "dumpDeprecationsFor" + suffix
    )
    val checkTask = registerCheckDeprecationsTask(
        shadowJarTaskName = "shadowJar", 
        suffix = "Jar", // the task name is "checkDeprecationsFor" + suffix
        expectedFileDoesNotExistMessage = "Run dumpDeprecationsForJar to create initial dump",
        checkFailureMessage = "Actually deprecated classes do not match expected ones. Please take a look.",
    )
    named("check") {
        dependsOn(checkTask)
    }
}
```