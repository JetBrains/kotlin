// swift-tools-version: 5.8
// NOTE: some of the configuration is shared with swiftBenchmark {} in build.gradle.kts
import PackageDescription

let package = Package(
    name: "swift-interop",
    products: [
        .executable(name: "swiftInterop", targets: ["SwiftInterop"])
    ],
    dependencies: [
        .package(path: "build/swiftpkg/benchmark")
    ],
    targets: [
        .executableTarget(
            name: "SwiftInterop",
            dependencies: [
                .product(name: "Benchmark", package: "benchmark")
            ],
            path: "swiftSrc"
        )
   ]
)