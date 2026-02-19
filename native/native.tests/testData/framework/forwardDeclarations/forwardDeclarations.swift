import ForwardDeclarations

private func test1() throws {
    let ptr = UnsafeMutableRawPointer(bitPattern: 0x1234)
    try assertEquals(actual: ForwardDeclarationsKt.sameForwardDeclaredStruct(ptr: ptr), expected: ptr)

    // We can't actually test this, because Swift can't import neither types nor functions due to
    // "interface/protocol '...' is incomplete":
    //
    // let classObj: ForwardDeclaredClass? = nil
    // try assertNil(LibKt.sameForwardDeclaredClass(obj: classObj))
    //
    // let protocolObj: ForwardDeclaredProtocol? = nil
    // try assertNil(LibKt.sameForwardDeclaredProtocol(obj: protocolObj))
}

// -------- Execution of the test --------

class ForwardDeclarationsTests : SimpleTestProvider {
    override init() {
        super.init()

        test("test1", test1)
    }
}
