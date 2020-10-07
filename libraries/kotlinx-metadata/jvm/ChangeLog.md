# kotlinx-metadata-jvm

## 0.1.1

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
