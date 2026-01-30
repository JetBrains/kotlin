// swift-tools-version: 5.8
// NOTE: some of the configuration is shared with swiftBenchmark {} in build.gradle.kts
import PackageDescription

let package = Package(
    name: "swift-interop",
    platforms: [.macOS(.v13)], // package-benchmark requirements
    dependencies: [
        .package(path: "build/swiftpkg/kt"),
        .package(url: "https://github.com/ordo-one/package-benchmark", .upToNextMajor(from: "1.4.0"))
    ],
    targets: [
        .executableTarget(
            name: "SwiftInterop",
            dependencies: [
                .product(name: "Kt", package: "kt"),
                .product(name: "Benchmark", package: "package-benchmark"),
            ],
            path: "Benchmarks/SwiftInterop",
            plugins: [
                .plugin(name: "BenchmarkPlugin", package: "package-benchmark")
            ]
        ),
   ]
)