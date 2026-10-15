// swift-tools-version: 6.1

import PackageDescription

let package = Package(
    name: "SpmLocalPackage",
    products: [
        .library(
            name: "SpmLocalPackageLibrary",
            targets: ["SpmLocalPackage"]
        )
    ],
    targets: [
        .target(name: "SpmLocalPackage")
    ]
)
