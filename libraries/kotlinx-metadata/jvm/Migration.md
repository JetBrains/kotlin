# Kotlinx-metadata migration guide

Starting with 0.6.0 release, Kotlin team is focused on revisiting and improving kotlinx-metadata-jvm API, with an aim to provide a stable release
in the near future. As a result, the API was reshaped, with cuts here and there, so we've provided a migration guide to help you with updates.

## Migrating from 0.6.x to 0.7.0

### Migration from Flags API to Attributes API

There are a lot of various modifiers that can be applied to various Kotlin declarations: `public`, `sealed`, `data`, `inline`, and so on.
Introspecting them is one of the major use cases for the kotlinx-metadata library.
In previous versions, they were represented as a bit mask and a `Flag.invoke` function:

```kotlin
fun nameOfPublicDataClass(kmClass: KmClass): ClassName? {
    return if (Flag.Common.IS_PUBLIC(kmClass.flags) && Flag.Class.IS_DATA(kmClass.flags)) kmClass.name else null
}
```

Such an API is based on implementation details and has problems, such as:

* Discoverability; it is hard to stumble across this API while looking into autocompletion pop-up for `kmClass.`.
* Non-OOP style and counterintuitivity; naturally, one wants to call something like `function.isPublic()`, and not `isPublic(function)`.
* Applicability and soundness; it is not a compiler error to call `Flag.IS_PUBLIC(kmType.flags)`, while `KmType` obviously does not have a notion of visibility.

To solve these problems, Flags API **is deprecated completely** for replacement with the new Attributes API.
Attributes API is fairly simple and essentially is a broad set of extensions on Km nodes,
such as `KmClass.visibility`, `KmClass.isData`, `KmFunction.isInline`, and so on.

For almost every deprecated `Flag` instance, there is a corresponding mutable extension property.
There are some exceptions to this rule, notably visibility and modality.
For them, all flags are replaced with a single extension that returns an enum value.
For example, `Flag.IS_PUBLIC(): Boolean` and `Flag.IS_PRIVATE(): Boolean`
are both replaced by `KmClass.visibility: Visibility` or `KmFunction.visibility: Visibility`.

For migration, replace `Flag` usages with access to corresponding extension properties.
Deprecation message for a particular `Flag` instance should help you identify a correct extension.

The function above can now be rewritten in a more clear and idiomatic way:

```kotlin
fun nameOfPublicDataClass(kmClass: KmClass): ClassName? {
    return if (kmClass.visibility == Visibility.PUBLIC && kmClass.isData) kmClass.name else null
}
```

### Changes in reading and writing API

After collecting some feedback from our users, we have decided to implement the following changes:

#### `KotlinClassMetadata.read()` now has a non-nullable return type of `KotlinClassMetadata`

Previously, `null` value was returned in case a metadata version was not compatible.
It was not very convenient, as `null` value does not state the exact version of the metadata and why it is incompatible (is it too old or too new).
Now, an `IllegalStateException` with appropriate message is thrown in this case.
To migrate, simply remove null-checks around `KotlinClassMetadata.read()`. In case you need special logic for the incompatible version case, add a try-catch block.

The same is applicable to `KotlinModuleMetadata.read()`.

#### Metadata validation moved from `toKmClass()` methods to `KotlinClassMetadata.read()`

Checks related to validation of metadata encoding that used to happen in conversion methods
(like `KotlinClassMetadata.Class.toKmClass()`, `KotlinClassMetadata.FileFacade.toKmPackage()`, etc)
are moved to the `KotlinClassMetadata.read()`.
As a result, these conversion methods are deprecated and replaced by the similarly named properties because they are no longer throw exceptions and simply return a cached result,
while actual conversion is moved to `KotlinClassMetadata.read()`.
To migrate, use provided replacements:

**Before:**
```kotlin
when (val metadata = KotlinClassMetadata.read(header)) {
    is KotlinClassMetadata.Class -> handleClass(metadata.toKmClass())
    is KotlinClassMetadata.FileFacade -> handleFileFacade(metadata.toKmPackage())
    is KotlinClassMetadata.MultiFileClassPart -> handleMFClassPart(metadata.facadeClassName, metadata.toKmPackage())
    ...
}
```

**After:**
```kotlin
when (val metadata = KotlinClassMetadata.read(header)) {
    is KotlinClassMetadata.Class -> handleClass(metadata.kmClass)
    is KotlinClassMetadata.FileFacade -> handleFileFacade(metadata.kmPackage)
    is KotlinClassMetadata.MultiFileClassPart -> handleMFClassPart(metadata.facadeClassName, metadata.kmPackage)
    ...
}
```

The same is applicable to `KotlinModuleMetadata.toKmModule()`.

#### Writing API returns the encoded result directly

`KotlinClassMetadata.writeClass` and other similar functions now return a `Metadata` instance directly
instead of returning a new `KotlinClassMetadata` instance.
Previous behavior caused confusion because it was not clear what operations are valid on a returned instance and 
how exactly it is supposed to be used.

As a result, `KotlinClassMetadata.annotationData: Metadata` property has been made private because there is no longer need for it to be exposed.
To migrate, simply remove `.annotationData` access from your writing logic:

**Before:**
```kotlin
fun save(kmClass: KmClass) {
    val metadata: Metadata = KotlinClassMetadata.writeClass(kmClass).annotationData
    writeToClassFile(metadata)
}
```

**After:**
```kotlin
fun save(kmClass: KmClass) {
    val metadata: Metadata = KotlinClassMetadata.writeClass(kmClass)
    writeToClassFile(metadata)
}
```

The same is applicable to `KotlinModuleMetadata.write`: it returns `ByteArray` directly.

## Migrating from 0.5.0 to 0.6.x

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
