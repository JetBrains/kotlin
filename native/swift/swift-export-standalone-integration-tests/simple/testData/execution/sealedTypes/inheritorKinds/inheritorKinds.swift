import KotlinRuntime
import SealedInheritorKinds
import Testing

// ---------- singleton `object` inheritor ----------

private func describeAppError(_ value: any AppError) -> String {
    switch value.sealedType() {
        case let .ioError(.fileReadError(type)): "fileRead: \(type.value.file)"
        case let .ioError(.databaseError(type)): "database: \(type.value.source)"
        case let .runtimeFailure(type): "runtime: \(type.value)"
    }
}

@Test
func testObjectInheritorHasItsOwnCase() throws {
    try #require(describeAppError(createRuntimeFailure()) == "runtime: RuntimeFailure")
}

@Test
func testNestedBranchMatchesUnderTheObjectSibling() throws {
    try #require(describeAppError(createFileReadError()) == "fileRead: example.txt")
}

@Test
func testObjectInheritorCaseCarriesTheSingleton() throws {
    guard case let .runtimeFailure(type) = createRuntimeFailure().sealedType() else {
        Issue.record("expected .runtimeFailure")
        return
    }
    // Asserted from Kotlin: `===` in Swift compares freshly built wrappers (KT-48137).
    try #require(isRuntimeFailureSingleton(value: type.value))
}


// ---------- `value class` inheritor ----------

@Test
func testValueClassInheritorHasItsOwnCase() throws {
    guard case let .wrappedInt(type) = createWrappedInt().sealedType() else {
        Issue.record("expected .wrappedInt")
        return
    }
    try #require(type.value.raw == 7)
}

@Test
func testValueClassSiblingIsUnaffected() throws {
    let result = switch createPlainHost().sealedType() {
        case let .wrappedInt(type): "wrapped: \(type.value.raw)"
        case let .plainHost(type): "plain: \(type.value)"
    }
    try #require(result == "plain: PlainHost")
}

// ---------- `fun interface` inheritor ----------

@Test
func testFunInterfaceInheritorReachedThroughExportedClass() throws {
    guard case let .eventCallback(type) = createCallbackImpl().sealedType() else {
        Issue.record("expected .eventCallback")
        return
    }
    try #require(type.value.onEvent() == "from exported class")
}

@Test
func testFunInterfaceInheritorReachedThroughSamLambda() throws {
    // The SAM-converted lambda's runtime class has no generated Swift class, which used to make
    // `sealedType()` trap (KT-88999).
    guard case let .eventCallback(type) = createSamCallback().sealedType() else {
        Issue.record("expected .eventCallback")
        return
    }
    try #require(type.value.onEvent() == "from SAM lambda")
}

@Test
func testFunInterfaceSiblingIsUnaffected() throws {
    let result = switch createPlainCallbackHost().sealedType() {
        case let .eventCallback(type): "callback: \(type.value.onEvent())"
        case let .plainCallbackHost(type): "plain: \(type.value)"
    }
    try #require(result == "plain: PlainCallbackHost")
}

// ---------- non-public root constructors ----------

private func describeFailure(_ value: Failure) -> String {
    switch value.sealedType() {
        case let .notFound(type): "notFound: \(type.value.message)"
        case let .timeout(type): "timeout: \(type.value.message)"
    }
}

@Test
func testInheritedConstructorParameterIsReachable() throws {
    // `Timeout` passes through the root's *protected* secondary constructor.
    try #require(describeFailure(createTimeout()) == "timeout: code=30")
}

@Test
func testLeafConstructorsArePublic() throws {
    // The leaves are constructible from Swift; the sealed roots are not (no public init).
    try #require(Timeout(seconds: 5).message == "code=5")
    try #require(NotFound().message == "not found")
    try #require(describeFailure(Timeout(seconds: 5)) == "timeout: code=5")
}

@Test
func testPrivateRootConstructorStillProducesACase() throws {
    let result = switch createDiskFullError().sealedType() {
        case let .diskFullError(type): "diskFullError: \(type.value.label)"
    }
    try #require(result == "diskFullError: via-private-ctor")
}

// ---------- generic sealed class ----------

private func describeQuery(_ value: Query) -> String {
    switch value.sealedType() {
        case .queryLoading: "loading"
        case let .queryValue(type): "value: \(type.value.value.map { String(describing: $0) } ?? "nil")"
        case let .queryFailure(type): "failure: \(type.value.message)"
    }
}

@Test
func testGenericSealedClassGetsAFullEnum() throws {
    try #require(describeQuery(createQueryLoading()) == "loading")
    try #require(describeQuery(createQueryValue()) == "value: payload")
    try #require(describeQuery(createQueryFailure()) == "failure: boom")
}
