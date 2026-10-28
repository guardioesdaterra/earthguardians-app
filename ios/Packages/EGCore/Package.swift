// swift-tools-version:5.9
// EGCore — pure Foundation logic for the Earth Guardians iOS app
// (models, parsers, site catalog, deep links, offline-first content repo).
// No iOS/UIKit/Compose dependencies so `swift test` runs on any macOS runner.
import PackageDescription

let package = Package(
    name: "EGCore",
    // Without an explicit `platforms` block SwiftPM defaults to a very old
    // macOS floor, which fails to compile Combine's ObservableObject/@Published.
    // Mac floor mirrors the toolchain the app builds on; iOS floor matches the
    // app's iOS 16 compat tier so Xcode doesn't downlevel the package.
    platforms: [
        .macOS(.v13),
        .iOS(.v16),
    ],
    products: [
        .library(name: "EGCore", targets: ["EGCore"]),
    ],
    targets: [
        .target(name: "EGCore"),
        .testTarget(name: "EGCoreTests", dependencies: ["EGCore"]),
    ]
)
