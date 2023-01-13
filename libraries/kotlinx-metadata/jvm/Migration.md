# Kotlinx-metadata migration guide

Starting with 0.6.0 release, Kotlin team is focused on revisiting and improving kotlinx-metadata API, with an aim to provide a stable release
in the near future. As a result, the API was reshaped, with cuts here and there, so we've provided a migration guide to help you with updates.

## Migrating from 0.5.0 to 0.6.0+

There are several significant changes between 0.5.0 and 0.6.0:

### Visitors are deprecated

There are two major APIs that allow introspecting Kotlin metadata: Visitor API (`KmClassVisitor`, `KmFunctionVisitor`, etc) and Nodes API
(`KmClass`, `KmFunction`, etc). After careful consideration, we've decided to deprecate Visitor API completely.
It is a more verbose and hard-to-use API that does not have any significant advantages. Everything that can be done using visitors can also be achieved with Nodes, usually with shorter and more idiomatic code.

As these APIs represent different paradigms, migration can't be automated and requires some manual work. In short, replace every `visitXxx` with access to corresponding property, e.g.
`KmClassVisitor.visitFunction(flags, name)` with `KmClass.functions`:

**Before:**
```kotlin
/**
 * Visitor that gets names of all public functions which start with 'test'
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
 * Extension function that gets names of all public functions which start with 'test'
 */
fun KmClass.testFunctions(): List<String> = this.functions.mapNotNull { f ->
    if (Flag.Common.IS_PUBLIC(f.flags) && f.name.startsWith("test")) f.name else null
}
```

As a result, complexity and line count are significantly reduced.
For a more sophisticated example, take a look at [this commit](https://github.com/JetBrains/kotlin/commit/c4d409608cf6b246a24f095bc7b30ff8d2efc373) that refactors an internal utility `kotlinp` which renders Kotlin metadata to text.

Note that for now, Km nodes still implement visitors (e.g. `KmClass` implements `KmClassVisitor`). This relation will be removed in the future.

### Metadata annotation class can be used directly

Parameter type of `KotlinClassMetadata.read` function was changed to `kotlin.Metadata` from `kotlinx.metadata.jvm.KotlinClassHeader`. 
It allows writing less boilerplate code as there's no need to copy parameters `d1`, `d2`, etc.

If you have obtained `Metadata` instance reflectively, you
can use it right away. In case you are reading binary metadata, you can create `Metadata` instance using
a [helper function](https://github.com/JetBrains/kotlin/blob/3d679b76bce04a9bfbb7c0a2f769d5838d2c3bf9/libraries/kotlinx-metadata/jvm/src/kotlinx/metadata/jvm/jvmMetadataUtil.kt#L27)
or by [directly calling the annotation constructor](https://kotlinlang.org/docs/annotations.html#instantiation).

> Note: as annotation instantiation is not available for Java clients, `KotlinClassHeader` is still present and reserved for construction from Java.
It implements `kotlin.Metadata` annotation interface, so they can be used interchangeably.

Additionally, the property `KotlinClassMetadata.header: KotlinClassHeader` was changed into `KotlinClassMetadata.annotationData: Metadata`, so it is 
also possible to use `Metadata` directly to write metadata back (see example in the section below).

### Writers are streamlined

To ease metadata manipulation, writing API was simplified. `KotlinClassMetadata.Class.Writer()` and other writers are deprecated;
Appropriate writer functions (e.g. `KotlinClassMetadata.writeClass`) should be used instead.

To migrate, simply replace calls to writers with new functions:

**Before:**
```kotlin
fun saveClass(kmClass: KmClass) {
    val writer = KotlinClassMetadata.Class.Writer()
    kmClass.accept(writer)
    val classMetadata: KotlinClassMetadata.Class = writer.write()
    val kotlinClassHeader: KotlinClassHeader = classMetadata.header
    
    // Write kotlinClassHeader.data1, data2, etc using ASM
}
```

**After:**
```kotlin
fun saveClass(kmClass: KmClass) {
    val classMetadata: KotlinClassMetadata.Class = KotlinClassMetadata.writeClass(kmClass)
    val metadata: Metadata = classMetadata.annotationData

    // Write Metadata.data1, data2, etc using ASM
}
```