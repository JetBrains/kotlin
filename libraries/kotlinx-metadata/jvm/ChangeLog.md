# kotlinx-metadata-jvm

## 2.0.0 and higher

Starting with Kotlin 2.0, kotlin-metadata-jvm library is promoted to stable, and is a part of Kotlin distribution now. 
It means that it has the same versioning as Kotlin compiler and Kotlin standard library, and the same release cycle.
To achieve this, coordinates of the library were changed: it is now in `org.jetbrains.kotlin` group with `kotlin-metadata-jvm` id (notice
the drop of `X` from the coordinates).
This also means that the root package was changed from `kotlinx.metadata` to `kotlin.metadata`.

Among other noticeable changes, all previously deprecated declarations were removed from the library.
In case you need to perform a migration with aid, migrate your project to 0.9.0 version first using [migration guide](Migration.md).

This is the last entry of this changelog. General changelog for Kotlin's `Libraries` subsystem is available in the `ChangeLog.md` at the repository root.

## 0.9.0

The main purpose of this release is to promote all previous deprecations to ERROR level if they were not already.
Please refer to the [migration guide](Migration.md) if you are using deprecated functions.
Also, this release includes several bugfixes. It still uses Kotlin 1.9, but is able to read or write metadata of version 2.0.

- Add missing documentation to `KmVersionRequirement.toString`
- Raise all deprecations in kotlinx-metadata-jvm to ERROR ([KT-63157](https://youtrack.jetbrains.com/issue/KT-63157))
- Do not allow writing metadata versions that are too high ([KT-64230](https://youtrack.jetbrains.com/issue/KT-64230))
- Add `KmVersionRequirementKind.UNKNOWN` ([KT-60870](https://youtrack.jetbrains.com/issue/KT-60870))

## 0.8.0

This release concludes our API overhaul: it features last significant API changes, as well as raised deprecations to ERROR level almost everywhere.
To help with migration, we've prepared a special [guide](Migration.md#migrating-from-070-to-080). 
It still uses Kotlin 1.9, but is able to read or write metadata of version 2.0.

- Provide a separate class for representing metadata version in kotlinx-metadata: `JvmMetadataVersion`
- Unify write() method and make it a member of `KotlinClassMetadata` (also `KotlinModuleMetadata`)
- Split `KotlinClassMetadata.read` into `readStrict` and `readLenient`
- Promote most deprecations in kotlinx-metadata-jvm to ERROR, including Flags API and Visitors API.
- Deprecate `KmProperty.hasGetter(hasSetter)` in favor of `KmProperty.getter(setter)`
- Add missing delegation in `KmDeclarationContainerVisitor.visitExtensions` for consistency

## 0.7.0

This release features several significant API changes. To help with migration, we've prepared a special [guide](Migration.md#migrating-from-06x-to-070).

- Update to Kotlin 1.9 with metadata version 1.9, support reading/writing metadata of version 2.0 which will be used in Kotlin 2.0
- Rework flags API (see [migration from Flags API to Attributes API](Migration.md#migration-from-flags-api-to-attributes-api)).
- Restructure `KotlinClass(Module)Metadata.write/read` (see [changes in reading and writing API](Migration.md#changes-in-reading-and-writing-api)).
- Add `@JvmStatic` + `@JvmOverloads` to writing functions in `KotlinClassMetadata`
- Deprecate `KmModule.annotations` for removal because it is always empty and should not be used.
- Move `KmModuleFragment` to an `kotlinx.metadata.internal.common` package. This class is intended for internal use only. If you have use-cases for it, please report an issue to YouTrack.
- Improve `toString()` for `KmAnnotationArgument`
- Add missing deprecation for `KmExtensionType` and experimentality for `KmConstantValue`.
- Enhance kotlinx-metadata-jvm KDoc and set up Dokka.

## 0.6.2

This release uses Kotlin 1.8.20 with metadata version 1.8, and as a special case, is able to read metadata of version 2.0. 
This is done as an incentive to test K2 compiler and 2.0 language version. 
No other changes were made and no migration is needed.
Note: 0.6.1 was released with an incorrect fix for this problem. Do not use 0.6.1.

## 0.6.0

This release features several significant API changes. To help with migration, we've prepared a special [guide](Migration.md#migrating-from-050-to-06x).

- Update to Kotlin 1.8 with metadata version 1.8, support reading/writing metadata of version 1.9 which will be used in Kotlin 1.9
- Deprecate Visitors API
- Replace usages of `KotlinClassHeader` with direct usage of `kotlin.Metadata` annotation. Former reserved exclusively for use from Java clients.
- `impl` package renamed to `internal`
- Writers are deprecated. Special function family `KotlinClassMetadata.write` is introduced instead.

## 0.5.0

- Update to Kotlin 1.7 with metadata version 1.7, support reading/writing metadata of version 1.8 which will be used in Kotlin 1.8.
- kotlinx-metadata-jvm can no longer be used on JVM 1.6, and now requires JVM 1.8 or later.
- Add `Flag.Type.IS_DEFINITELY_NON_NULL`.
- Add `KmClass.contextReceiverTypes`, `KmFunction.contextReceiverTypes`, `KmProperty.contextReceiverTypes`
  - The API is experimental and requires `@ExperimentalContextReceivers` on the usages.

## 0.4.2

- Add experimental internal API to read metadata of `.kotlin_builtins`/`.kotlin_metadata` files.

## 0.4.1

- Add `KmProperty.syntheticMethodForDelegate` for optimized delegated properties (KT-39055).

## 0.4.0

- Update to Kotlin 1.6 with metadata version 1.6, support reading/writing metadata of version 1.7 which will be used in Kotlin 1.7.
- Add `JvmPropertyExtensionVisitor.visitSyntheticMethodForDelegate` for optimized delegated properties (KT-39055).
- Add JVM-specific class flags:
  - `JvmClassExtensionVisitor.visitJvmFlags`
  - `JvmFlag.Class.HAS_METHOD_BODIES_IN_INTERFACE`
  - `JvmFlag.Class.IS_COMPILED_IN_COMPATIBILITY_MODE`
- [`KT-48965`](https://youtrack.jetbrains.com/issue/KT-48965) Make the type of `KmValueParameter.type` non-null `KmType`
- Remove unused `JvmTypeAliasExtensionVisitor` and `JvmValueParameterExtensionVisitor`
- Fix type flags (suspend, definitely non-null) on underlying type of inline class available via `KmClass.inlineClassUnderlyingType`

## 0.3.0

- Update to Kotlin 1.5 with metadata version 1.5.
  Note: metadata of version 1.5 is readable by Kotlin compiler/reflection of versions 1.4 and later.
- Breaking change: improve API of annotation arguments.
  `KmAnnotationArgument` doesn't have `val value: T` anymore, it was moved to a subclass named `KmAnnotationArgument.LiteralValue<T>`.
  The property `value` is:
  - renamed to `annotation` in `AnnotationValue`
  - renamed to `elements` in `ArrayValue`
  - removed in favor of `enumClassName`/`enumEntryName` in `EnumValue`
  - removed in favor of `className`/`arrayDimensionCount` in `KClassValue`
  - changed type from signed to unsigned integer types in `UByteValue`, `UShortValue`, `UIntValue`, `ULongValue`
- [`KT-44783`](https://youtrack.jetbrains.com/issue/KT-44783) Add Flag.IS_VALUE for value classes
  - Breaking change: `Flag.IS_INLINE` is deprecated, use `Flag.IS_VALUE` instead
- Breaking change: deprecate `KotlinClassHeader.bytecodeVersion` and `KotlinClassHeader`'s constructor that takes a bytecode version array.
  Related to ['KT-41758`](https://youtrack.jetbrains.com/issue/KT-41758).
- [`KT-45594`](https://youtrack.jetbrains.com/issue/KT-45594) KClass annotation argument containing array of classes is not read/written correctly
- [`KT-45635`](https://youtrack.jetbrains.com/issue/KT-45635) Add underlying property name & type for inline classes

## 0.2.0

- ['KT-41011`](https://youtrack.jetbrains.com/issue/KT-41011) Using KotlinClassMetadata.Class.Writer with metadata version < 1.4 will write incorrect version requirement table
    - Breaking change: `KotlinClassMetadata.*.Writer.write` throws exception on `metadataVersion` earlier than 1.4.0.
      Note: metadata of version 1.4 is readable by Kotlin compiler/reflection of versions 1.3 and later.
- Breaking change: `KotlinClassMetadata.*.Writer.write` no longer accept `bytecodeVersion`.
- [`KT-42429`](https://youtrack.jetbrains.com/issue/KT-42429) Wrong interpretation of Flag.Constructor.IS_PRIMARY
    - Breaking change: `Flag.Constructor.IS_PRIMARY` is deprecated, use `Flag.Constructor.IS_SECONDARY` instead
- [`KT-37421`](https://youtrack.jetbrains.com/issue/KT-37421) Add Flag.Class.IS_FUN for functional interfaces
- Add `KmModule.optionalAnnotationClasses` for the new scheme of compilation of OptionalExpectation annotations in multiplatform projects ([KT-38652](https://youtrack.jetbrains.com/issue/KT-38652))

## 0.1.0

- [`KT-26602`](https://youtrack.jetbrains.com/issue/KT-26602) Provide a value-based API

## 0.0.6

- [`KT-31308`](https://youtrack.jetbrains.com/issue/KT-31308) Add module name extensions to kotlinx-metadata-jvm
- [`KT-31338`](https://youtrack.jetbrains.com/issue/KT-31338) Retain "is moved from interface companion" property flag in kotlinx-metadata-jvm
    - Breaking change: JvmPropertyExtensionVisitor.visit has a new parameter `jvmFlags: Flags`
- Correctly write "null" constant value in effect expression of a contract
- Rename `desc` parameters to `signature` in JvmFunctionExtensionVisitor, JvmPropertyExtensionVisitor, JvmConstructorExtensionVisitor
- Do not expose KmExtensionType internals
- Add KmExtensionVisitor.type to get dynamic type of an extension visitor

## 0.0.5

- [`KT-25371`](https://youtrack.jetbrains.com/issue/KT-25371) Support unsigned integers in kotlinx-metadata-jvm
- [`KT-28682`](https://youtrack.jetbrains.com/issue/KT-28682) Wrong character replacement in ClassName.jvmInternalName of kotlinx-metadata-jvm

## 0.0.4

- [`KT-25920`](https://youtrack.jetbrains.com/issue/KT-25920) Compile kotlinx-metadata-jvm with JVM target bytecode version 1.6 instead of 1.8
- [`KT-25223`](https://youtrack.jetbrains.com/issue/KT-25223) Add JvmFunctionExtensionVisitor.visitEnd
- [`KT-26188`](https://youtrack.jetbrains.com/issue/KT-26188) Do not pass field signature for accessor-only properties

## 0.0.3

- Support metadata of local delegated properties (see `JvmDeclarationContainerExtensionVisitor.visitLocalDelegatedProperty`)
- [`KT-24881`](https://youtrack.jetbrains.com/issue/KT-24881) Use correct class loader in kotlinx-metadata to load MetadataExtensions implementations
- [`KT-24945`](https://youtrack.jetbrains.com/issue/KT-24945) Relocate package org.jetbrains.kotlin to fix IllegalAccessError in annotation processing

## 0.0.2

- Change group ID from `org.jetbrains.kotlin` to `org.jetbrains.kotlinx`
- Depend on a specific version of kotlin-stdlib from Maven Central instead of snapshot from Sonatype Nexus
- Use `JvmMethodSignature` and `JvmFieldSignature` to represent JVM signatures instead of plain strings

## 0.0.1

- Initial release
