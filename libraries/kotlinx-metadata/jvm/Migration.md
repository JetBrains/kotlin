# Kotlinx-metadata migration guide

Starting with 0.6.0 release, Kotlin team is focused on revisiting and improving kotlinx-metadata API, with an aim to provide a stable 1.0.0 release
in the near future. As a result, an API was reshaped, with cuts here and there, so we've provided a migration guide to help you with updates.

## Migrating from 0.5.0 to 0.6.0+

There are several significant changes between 0.5.0 and 0.6.0-RC:

### Visitors are deprecated

There are two major APIs that allow introspecting Kotlin metadata: Visitor API (KmClassVisitor, KmFunctionVisitor, etc) and KmNodes API
(KmClass, KmFunction, etc). After careful consideration, we've decided to deprecate Visitor API completely.
It is more verbose and hard to use API that does not have any significant advantages. Everything that can be done using visitors is also possible to do with Nodes.

As these APIs represent different paradigms, migration can't be automated and requires some manual work. In short, replace every `visitXxx` with access to corresponding property, e.g.
`KmClassVisitor.visitFunction(flags, name)` with `KmClass.functions`:

**Before:**
```kotlin
// BEFORE:
/**
 * Visitor that gets all public functions names which start with 'test'
 */
class TestFunctionFinder : KmClassVisitor() {
    val result = mutableListOf<String>()

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        if (Flag.Common.IS_PUBLIC(flags) && name.startsWith("test")) result.add(name)
        return null
    }
}

// Helper function to invoke visitor
fun KmClass.testFunctions(): List<String> = TestFunctionFinder().also { this.accept(it) }.result.toList()
```
**After:**
```kotlin
/**
 * Extension function that gets all public functions names which start with 'test'
 */
fun KmClass.testFunctions(): List<String> = this.functions.mapNotNull { f ->
    if (Flag.Common.IS_PUBLIC(f.flags) && f.name.startsWith("test")) f.name else null
}
```

As you can see, complexity and line count are significantly reduced.
If you need more sophisticated example, take a look at [this commit](https://github.com/JetBrains/kotlin/commit/c4d409608cf6b246a24f095bc7b30ff8d2efc373) that refactors KotlinP utility.

Note that for now, Km nodes still implement visitors (e.g. `KmClass` implements `KmClassVisitor`). This relation will be removed in the future.

### Metadata annotation class can be used directly

Parameter type of `KotlinClassMetadata.read` function was changed to `kotlin.Metadata` from `kotlinx.metadata.jvm.KotlinClassMetadata`. 
It allows writing less boilerplate code as there's no need to copy parameters `d1`, `d2`, etc.

If you have obtained `Metadata` instance reflectively, you
can use it right away. In case you are reading binary metadata, you can create `Metadata` instance using
[helper function](https://github.com/JetBrains/kotlin/blob/3d679b76bce04a9bfbb7c0a2f769d5838d2c3bf9/libraries/kotlinx-metadata/jvm/src/kotlinx/metadata/jvm/jvmMetadataUtil.kt#L27)
or by [directly calling annotation constructor](https://kotlinlang.org/docs/annotations.html#instantiation).

> Note: as annotation instantiation is not available for Java clients, `KotlinClassMetadata` is still present and reserved for construction from Java.
It implements `kotlin.Metadata` annotation interface, so they can be used interchangeably.

Additionally, property `KotlinClassMetadata.header: KotlinClassHeader` was changed into `KotlinClassMetadata.annotationData: Metadata`, so it is 
also possible to use `Metadata` directly to write metadata back.

### Impl package moved to internal

To better communicate intentions and nature of classes in the former `impl` package, all of them were moved to the `internal` package.
It means that they are not recommended for external usage as there is no documentation nor any compatibility guarantees.

If you need to migrate, replace all `import kotlinx.metadata.impl.*` to `import kotlinx.metadata.internal.*`
and `import kotlinx.metadata.jvm.impl.*` to `import kotlinx.metadata.jvm.internal.*`.
However, we advise you not to use these classes and create a YouTrack ticket instead if you need some specific public API.

### Writers are streamlined

To ease metadata manipulation, writing API was simplified. `KotlinClassMetadata.Class.Writer()` and other writers are deprecated;
Appropriate writer functions (e.g. `KotlinClassMetadata.writeClass`) should be used instead.