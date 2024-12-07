// swift-tools-version: 5.9

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
