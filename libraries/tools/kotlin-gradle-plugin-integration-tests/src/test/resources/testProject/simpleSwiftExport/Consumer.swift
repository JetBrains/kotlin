@testable import Shared

#if arch(arm64)
com.github.jetbrains.swiftexport.iosSimulatorArm64Bar()
#elseif arch(x86_64)
com.github.jetbrains.swiftexport.iosX64Bar()
#else
#error("Not supposed to happen")
#endif