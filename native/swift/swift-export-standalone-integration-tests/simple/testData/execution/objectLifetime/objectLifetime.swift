import Foundation
import KotlinRuntime
import ObjectLifetime
import Testing

// Every case checks both directions of the ownership balance:
// the Kotlin object stays alive while only Swift references it (no over-release),
// and is collected once Swift drops it (no leak).
@Suite(.serialized)
struct ObjectLifetimeTests {
    final class SwiftDerived: Base {
        override func ping() -> Int32 { 42 }
    }

    @Test
    func objectConstructedInSwiftIsReleased() throws {
        try autoreleasepool {
            let bare = Bare(id: 1)
            trackBare(bare: bare)
            try withExtendedLifetime(bare) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // A fresh class wrapper adopting the returned reference.
    @Test
    func objectReturnedFromKotlinIsReleased() throws {
        try autoreleasepool {
            let bare = makeBare()
            try withExtendedLifetime(bare) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    @Test
    func objectPassedBackToKotlinIsReleased() throws {
        try autoreleasepool {
            let bare = makeBare()
            trackBare(bare: bare)
            try withExtendedLifetime(bare) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // The second and later returns substitute the cached wrapper for the freshly allocated one.
    @Test
    func objectReturnedRepeatedlyIsReleased() throws {
        try autoreleasepool {
            holdBare()
            let first = heldBare()
            for _ in 0..<100 {
                try #require(heldBare() === first)
            }
            dropHeld()
            try withExtendedLifetime(first) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // The protocol wrapper factory picking the cached best-fitting class.
    @Test
    func exportedInterfaceImplementationIsReleased() throws {
        try autoreleasepool {
            let marker = makeExportedMarker()
            try #require(marker is ExportedMarker)
            try withExtendedLifetime(marker) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // The protocol wrapper factory falling back to an uncached existential wrapper.
    @Test
    func hiddenInterfaceImplementationIsReleased() throws {
        try autoreleasepool {
            let marker = makeHiddenMarker()
            try #require(!(marker is ExportedMarker))
            try withExtendedLifetime(marker) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    @Test
    func objectReturnedAsAnyIsReleased() throws {
        try autoreleasepool {
            let any = makeAny()
            try #require(any is Bare)
            try withExtendedLifetime(any) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // Kotlin hands back the Swift instance bound to the object.
    @Test
    func swiftSubclassReturnedFromKotlinIsReleased() throws {
        try autoreleasepool {
            let derived = SwiftDerived()
            holdBase(base: derived)
            for _ in 0..<100 {
                try #require(heldBase() === derived)
            }
            dropHeld()
            try withExtendedLifetime(derived) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // Each Kotlin -> Swift override call wraps `self` through a reverse bridge.
    @Test
    func swiftSubclassCalledFromKotlinIsReleased() throws {
        try autoreleasepool {
            let derived = SwiftDerived()
            holdBase(base: derived)
            dropHeld()
            for _ in 0..<100 {
                try #require(callPing(base: derived) == 42)
            }
            try withExtendedLifetime(derived) { try #require(isTrackedAlive()) }
        }
        try #require(!isTrackedAlive())
    }

    // Permanent objects get a fresh uncached wrapper per return, whose lifetime is the plain NSObject refcount.
    @Test
    func permanentObjectWrappersAreBalanced() throws {
        try autoreleasepool {
            let first = unitAsAny()
            try #require(isPermanentObject(value: first))
            for _ in 0..<100 {
                try #require(isPermanentObject(value: unitAsAny()))
            }
            try #require(first is KotlinBase)
        }
    }
}
