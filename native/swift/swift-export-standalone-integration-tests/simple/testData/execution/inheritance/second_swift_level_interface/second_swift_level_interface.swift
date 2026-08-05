import Inheritance
import Testing

// Level 1 — the only place the Kotlin interface is adopted and implemented.
class SwiftFirstLevel: InterfaceAnchor, SecondLevelContract {
    func token() -> String { "swift-token" }
}

// Level 2 — adds nothing at all: both the conformance and the implementation are inherited from level 1.
final class SwiftSecondLevel: SwiftFirstLevel {}

// Control: with the instance at the same level that declares the conformance, the itable slot resolves.
// If this one starts failing too, the problem is not specific to the second level.

// Control: with the instance at the same level that declares the conformance, the itable slot resolves.
// If this one starts failing too, the problem is not specific to the second level.
@Test
func kotlinCallsInterfaceMethodOnFirstSwiftLevel() throws {
    let value = SwiftFirstLevel()
    let contractView: any SecondLevelContract = value
    #expect(contractView === value)
    #expect(callToken(value: value) == "swift-token")
}

// The reproducer: identical call, one extra Swift inheritance level between the instance and the class that
// adopted the Kotlin interface. Remove `.disabled(...)` to reproduce.
@Test(.disabled("KT-88042: Kotlin cannot dispatch an itable slot to a Swift implementation inherited from the first Swift level"))
func kotlinCallsInterfaceMethodOnSecondSwiftLevel() throws {
    let value = SwiftSecondLevel()
    let contractView: any SecondLevelContract = value
    #expect(contractView === value)
    #expect(callToken(value: value) == "swift-token")
}
