# kotlinx-metadata-klib migration guide

## Kotlin 2.5.20: preparing for KLIBs without the metadata header file

### What is changing in KLIBs

Every KLIB currently contains a metadata header file, `default/linkdata/module`. It is a serialized
`KlibMetadataProtoBuf.Header` with three pieces of information:

- `moduleName` — the module name, which, as established in KT-87573, is always the KLIB's `unique_name` manifest
  property wrapped in angle brackets (for example, `<foo.bar.my-lib>` for `unique_name = foo.bar.my-lib`);
- `packageFragmentName` — the list of packages whose fragments the KLIB contains;
- `emptyPackage` — the list of packages that have no declarations.

All of it is derivable from other parts of the KLIB: the name from the manifest, the package list from the directory
names under `default/linkdata/`. Therefore, starting with Kotlin 2.6.0, the compiler is going to stop writing the
header file ([KT-87197](https://youtrack.jetbrains.com/issue/KT-87197)). KLIBs produced by that compiler will have no
`default/linkdata/module`, and `kotlinx-metadata-klib` has to be able to read them.

### What is changing in kotlinx-metadata-klib

Starting with the release built from Kotlin 2.5.20, `kotlinx-metadata-klib` is prepared for header-less KLIBs
([KT-87573](https://youtrack.jetbrains.com/issue/KT-87573)):

- `KlibModuleMetadata.MetadataLibraryProvider.moduleHeaderData` is now `ByteArray?`. An implementation returns `null`
  when the KLIB has no header file.
- `KlibModuleMetadata.MetadataLibraryProvider` has a new abstract member `packageFqNames: Set<String>`, the fully-qualified
  names of the packages whose fragments the KLIB contains, with `""` standing for the root package. It replaces the
  package list that used to be read from the header.
- `KlibModuleMetadata.readStrict()` and `KlibModuleMetadata.readLenient()` no longer fail when the header is absent.
- `KlibModuleMetadata.name` is deprecated and is now `String?`. It is `null` when the metadata was read from a KLIB
  without a header file.
- `KlibModuleMetadata.SerializedKlibMetadata.header` is now `ByteArray?`.
- `KlibModuleMetadata.write()` still writes the header and throws `IllegalStateException` when `name` is `null`.

These changes are neither source- nor binary-compatible for code that implements `MetadataLibraryProvider`
or reads `KlibModuleMetadata.name` as a non-null `String`.

### How to migrate

#### Implementations of `MetadataLibraryProvider`

Implement `packageFqNames` by listing `default/linkdata/`: `root_package` is the root package (`""`), `package_<fqName>`
is the package `<fqName>`. Return `null` from `moduleHeaderData` when `default/linkdata/module` is absent.

#### Readers of `KlibModuleMetadata.name`

Read the `unique_name` property from the `default/manifest` file (`java.util.Properties` format) instead. Unlike the
header value, it is not wrapped in angle brackets.

#### Writers of `KlibModuleMetadata`

`write()` still needs a non-null `name`. For metadata read without a header, construct a new `KlibModuleMetadata`
with the name from the manifest and the fragments of the read instance.
