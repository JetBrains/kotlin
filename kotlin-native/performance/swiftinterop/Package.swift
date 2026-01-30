// swift-tools-version: 5.8
// NOTE: some of the configuration is shared with swiftBenchmark {} in build.gradle.kts
import PackageDescription

let package = Package(
    name: "swift-interop",
    products: [
        .executable(name: "swiftInterop", targets: ["SwiftInterop"])
    ],
    dependencies: [
        .package(path: "build/swiftpkg/kt")
    ],
    targets: [
        .executableTarget(
            name: "SwiftInterop",
            dependencies: [
                .product(name: "Kt", package: "kt")
            ],
            path: "swiftSrc"
        )
   ]
)