// swift-tools-version: 5.8
import PackageDescription

let package = Package(
    name: "SharedPackage",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "SharedPackage",
            targets: ["Shared"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "./Shared.xcframework"
        )
    ]
)
