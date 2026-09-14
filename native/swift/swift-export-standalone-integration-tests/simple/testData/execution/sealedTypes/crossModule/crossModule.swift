import KotlinRuntime
import SealedCrossModuleBase
import SealedCrossModuleMain
import Testing

private func describe(_ error: any RootError) -> String {
    // No `unknown` case: every direct inheritor of the root is exported.
    switch error.sealedType() {
        case let .fatalError(type): "fatal: \(type.value)"
        case let .recoverableError(type): "recoverable: \(type.value)"
        case let .leafInterface(type): "leaf: \(type.value)"
        case let .transportError(.timeoutError(type)): "timeout: \(type.value)"
        case let .transportError(.unknown(type)): "transport-unknown: \(type.value)"
    }
}

@Test
func testInheritorsDeclaredInTheBaseModule() throws {
    try #require(describe(createFatalError()) == "fatal: FatalError")
    try #require(describe(createRecoverableError()) == "recoverable: RecoverableError")
    try #require(describe(createLeafImplInBase()) == "leaf: LeafImplInBase")
}

@Test
func testOutOfModuleSubclassOfAnOpenInheritorReportsTheOpenInheritorsCase() throws {
    // The switch above is exhaustive without `default`, yet this value's class was compiled into a
    // different module — the known hole in the exhaustiveness guarantee.
    try #require(describe(createSubclassInMain()) == "recoverable: SubclassInMain")
}

@Test
func testOutOfModuleImplementationOfALeafInterfaceReportsTheInterfacesCase() throws {
    try #require(describe(createLeafImplInMain()) == "leaf: LeafImplInMain")
}

@Test
func testCrossModuleSealedTypeCrossesTheBoundaryAsAParameter() throws {
    try #require(rootAsParameter(error: createLeafImplInBase()) == "root:LeafImplInBase")
    try #require(rootAsParameter(error: createSubclassInMain()) == "root:SubclassInMain")
}

@Test
func testSealedIntermediateBranchNestsAcrossTheModuleBoundary() throws {
    try #require(describe(createTimeoutError()) == "timeout: TimeoutError")
}

@Test
func testNonExportedInheritorOfTheIntermediateOnlyAffectsTheIntermediatesEnum() throws {
    // `unknown` sits on `TransportError_SealedType`, not on `RootError_SealedType`: the root's own
    // direct inheritors are all exported, so the outer switch above needs no `unknown` case.
    try #require(describe(createHiddenTransportError()) == "transport-unknown: HiddenTransportError")
}
