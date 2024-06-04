// swift-tools-version: 5.10
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "FrameworkConsumer",
    products: [
        .library(
            name: "FrameworkConsumer",
            targets: ["FrameworkConsumer"]),
    ],
    targets: [
        .target(name: "FrameworkConsumer", dependencies: ["Shared"]),
        .binaryTarget(name: "Shared", path: "Shared.xcframework")
    ]
)
