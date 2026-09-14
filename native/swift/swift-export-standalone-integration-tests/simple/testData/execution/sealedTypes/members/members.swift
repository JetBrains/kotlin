import KotlinRuntime
import SealedMembers
import Testing

private func payload(_ value: Account) -> String {
    switch value.sealedType() {
        case let .personal(type): "personal: \(type.value.id)/\(type.value.status.description)"
        case let .business(type): "business: \(type.value.id)/\(type.value.status.description)"
    }
}

@Test
func testOverriddenPropertiesAreReadableThroughThePayload() throws {
    try #require(payload(createPersonal()) == "personal: p-1/CLOSED")
    try #require(payload(createBusiness()) == "business: b-1/ACTIVE")
}

@Test
func testOverriddenFunctionDispatchesThroughTheRoot() throws {
    try #require(createPersonal().label() == "personal:p-1")
    try #require(createBusiness().label() == "business:b-1")
    try #require(createSubsidiary().label() == "subsidiary:s-1")
}

@Test
func testIndirectSubclassReportsTheOpenInheritorsCase() throws {
    try #require(payload(createSubsidiary()) == "business: s-1/CLOSED")
}
