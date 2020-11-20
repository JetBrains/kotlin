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

There are few limitations in the current version of KLIB Commonizer:
* It supports only interop libraries shipped with Kotlin/Native. It doesn't support KLIBs that are produced from C-interop or Kotlin code.
* It works only for a native source set that is shared among platform-specific source sets and these source sets themselves. It doesn't work for native source sets shared at higher levels of the source set hierarchy. For example, if you have `nativeDarwinMain` that includes `iosMain` with `iosArm64Main` and `iosX64Main`, and `watchosDeviceMain` with `watchosArm64Main` and `watchosArm32Main`, the KLIB Commonizer will work separately for `iosMain` and `watchosDeviceMain` and won't work for `nativeDarwinMain`.
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

* It does not process targets that are not available at the current host machine. For example, if you have a project with `nativeMain` source set that includes `macosX64Main`, `linuxX64Main` and `mingwX64Main`, and you run the KLIB Commonizer for this project on MacOS machine, then `mingwX64Main` source set will not be processed as far as `mingw_x64` target is absent in Kotlin/Native distribution for MacOS. The KLIB Commonizer will print the appropriate warning message and will work only for `macosX64Main` and `linuxX64Main` source sets. You can find the list of targets supported on various hosts here: https://kotlinlang.org/docs/reference/mpp-supported-platforms.html
   ```
   Kotlin KLIB commonizer: Please wait while preparing libraries.
   [Step 1 of 1] Preparing commonized Kotlin/Native libraries for targets [macos_x64, linux_x64, mingw_x64] (137 items)
     Warning: No platform libraries found for target mingw_x64. This target will be excluded from commonization.
     ...
   ```
   In the degenerate case when all but one targets are not available at the host machine, the KLIB Commonizer is not launched.

