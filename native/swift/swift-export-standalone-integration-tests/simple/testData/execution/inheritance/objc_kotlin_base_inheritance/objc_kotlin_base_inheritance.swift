import Inheritance
import Testing
import KotlinRuntime

// The Objective-C classes come from ObjCLeaf.h/.m, compiled by Clang and exposed here through a
// bridging header. They inherit `KotlinBase` directly and have no Swift metadata of their own.

@Test
func objCClassInheritsKotlinBaseDirectly() throws {
    let leaf = ObjCLeaf()
    #expect(leaf.label == "objc-leaf")

    // It has a real Kotlin counterpart, so it crosses the bridge as `Any` and keeps its identity.
    #expect(echoAny(value: leaf) as AnyObject === leaf)
    #expect(areSame(lhs: leaf, rhs: leaf))

    let storage = AnyStorage()
    storage.store(value: leaf)
    #expect(try #require(storage.retrieve()) as AnyObject === leaf)
}

@Test
func objCClassIsConstructibleWithNew() throws {
    // `+new` routes through `-init`; `KotlinBase` no longer declares either unavailable.
    let leaf = try #require(makeObjCLeafWithNew())
    #expect(leaf.label == "objc-leaf")
    #expect(echoAny(value: leaf) as AnyObject === leaf)
}

@Test
func secondObjCLevelBelowKotlinBaseGetsItsOwnCounterpart() throws {
    let deeper = ObjCDeeperLeaf()
    #expect(deeper.label == "objc-deeper-leaf")
    #expect(echoAny(value: deeper) as AnyObject === deeper)

    let shallow = ObjCLeaf()
    #expect(!areSame(lhs: shallow, rhs: deeper))
}

@Test
func objCInstancesHaveAnySemanticsInKotlin() throws {
    let one = ObjCLeaf()
    let two = ObjCLeaf()

    #expect(areEqual(lhs: one, rhs: one))
    #expect(!areEqual(lhs: one, rhs: two))
    #expect(one.hash == Int(hashOf(value: one)))
}
