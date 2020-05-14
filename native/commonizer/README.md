# KLIB Commonizer

You can use platform-dependent libraries, such as `Foundation`, `UIKit`, `posix`, in source sets shared among several native targets. For each project that has native source sets that depend on a shared _native_ source set, the special tool KLIB Commonizer automatically produces:
* One library in the KLIB format (`*.klib`) with the common API of the library that includes declarations that are identical among all platforms and `expect` declarations for the APIs that differ from platform to platform.
* One KLIB for each platform-specific source set that contains the `actual` declarations and the declarations that are available only on this particular platform.

```
                    commonMain
                        |
                        |
                     iosMain ----------▶ Foundation (common)
                      /   \              ┌──────────────────┐
                     /     \             │ expect class ... │
         iosArm64Main      iosX64Main    │ expect fun ...   │
                |             |          └──────────────────┘
                |             |
                ▼             ▼
Foundation (ios_arm64)     Foundation (ios_x64)
┌────────────────────┐     ┌──────────────────┐
│ actual class ...   │     │ actual class ... │
│ actual fun ...     │     │ actual fun ...   │
└────────────────────┘     └──────────────────┘
```

The resulting KLIBs are automatically added to the dependencies of the corresponding shared native and platform-specific source sets.

There are a few limitations in the current version of Klib Commonizer:
* It supports only interop libraries shipped with Kotlin/Native. It doesn’t support KLIBs that are produced from C-interop or Kotlin code.
* It works only for a native source set that is shared among platform-specific source sets and these source sets themselves. It doesn’t work for native source sets shared at higher levels of the source set hierarchy. For example, if you have `nativeDarwinMain` that includes `iosMain` with `iosArm64Main` and `iosX64Main`, and `watchosDeviceMain` with `watchosArm64Main` and `watchosArm32Main`, the KLIB Commonizer will work for separately `iosMain` and `watchosDeviceMain` and won’t work for `nativeDarwinMain`.

```
                  commonMain
                      |
                      |
               nativeDarwinMain    <--- Commonizer is NOT applied
                  /       \
                 /         \
          iosMain         watchosDeviceMain  <--- Commonizer is applied
           /   \                /  \
          /     \              ..  ..
iosArm64Main   iosX64Main 
```