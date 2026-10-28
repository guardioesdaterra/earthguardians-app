// swift-tools-version:5.9
// EGCore — pure Foundation logic for the Earth Guardians iOS app
// (models, parsers, site catalog, deep links, offline-first content repo).
// No iOS/UIKit/Compose dependencies so `swift test` runs on any macOS runner.
import PackageDescription

let package = Package(
    name: "EGCore",
    products: [
        .library(name: "EGCore", targets: ["EGCore"]),
    ],
    targets: [
        .target(name: "EGCore"),
        .testTarget(name: "EGCoreTests", dependencies: ["EGCore"]),
    ]
)
