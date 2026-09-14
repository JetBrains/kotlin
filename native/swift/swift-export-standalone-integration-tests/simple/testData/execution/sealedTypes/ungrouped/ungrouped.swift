import KotlinRuntime
import SealedTypes
import Testing

@Test
func testClassC_SealedInterfaceA() throws {
    let value = createClassC_SealedInterfaceA()
    var result = switch value.sealedType() {
        case let .sealedClassA(type): "sealedClassA: \(type.value)"
        case let .classE(type): "classE: \(type.value)"
        case let .sealedInterfaceB(type): "sealedInterfaceB: \(type.value)"
        case let .sealedInterfaceD(type): "sealedInterfaceD: \(type.value)"
        case let .interfaceC(type): "interfaceC: \(type.value)"
        case let .unknown(type): "unknown: \(type)"
    }
    try #require(result == "sealedClassA: ClassC")
    result = switch value.sealedType() {
        case let .sealedClassA(.classC(type)): "classC: \(type.value)"
        case let .sealedClassA(type): "sealedClassA: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "classC: ClassC")
}

@Test
func testClassC_SealedClassA() throws {
    let value = createClassC_SealedClassA()
    let result = switch value.sealedType() {
        case let .sealedClassB(.classD(type)): "classD: \(type.value)"
        case let .classC(type): "classC: \(type.value)"
        case let .classJ(type): "classJ: \(type.value)"
        case let .unknown(type): "unknown: \(type)"
    }
    try #require(result == "classC: ClassC")
}

@Test
func testClassD_SealedClassB() throws {
    let value = createClassD_SealedClassB()
    let result = switch value.sealedType() {
        case let .classD(type): "classD: \(type.value)"
    }
    try #require(result == "classD: ClassD")
}

@Test
func testClassE_SealedInterfaceA() throws {
    let value = createClassE_SealedInterfaceA()
    var result = switch value.sealedType() {
        case let .classE(type): "classE: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "classE: ClassE")
    result = switch value.sealedType() {
        case let .sealedClassA(type): "sealedClassA: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "default: ClassE")
}

@Test
func testClassF_SealedInterfaceA() throws {
    let value = createClassF_SealedInterfaceA()
    let result = switch value.sealedType() {
        case let .unknown(type): "unknown: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "unknown: ClassF")
}

@Test
func testClassG_SealedClassA() throws {
    let value = createClassG_SealedClassA()
    let result = switch value.sealedType() {
        case let .unknown(type): "unknown: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "unknown: ClassG")
}

@Test
func testPrivateInterfaceCImpl_SealedInterfaceA() throws {
    let value = createPrivateInterfaceCImpl_SealedInterfaceA()
    let result = switch value.sealedType() {
        case let .interfaceC(type): "interfaceC: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "interfaceC: PrivateInterfaceCImpl")
}

@Test
func testAnonymousInterfaceC_SealedInterfaceA() throws {
    let value = createAnonymousInterfaceC_SealedInterfaceA()
    let result = switch value.sealedType() {
        case let .interfaceC(type): "interfaceC: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "interfaceC: AnonymousInterfaceC")
}

@Test
func testEnumClassA_SealedInterfaceA() throws {
    let value = createEnumClassA_SealedInterfaceA()
    let result = switch value.sealedType() {
        case let .unknown(type): "unknown: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "unknown: ONE")
}

@Test
func testEnumClassB_SealedInterfaceA() throws {
    let value = createEnumClassB_SealedInterfaceA()
    let result = switch value.sealedType() {
        case let .interfaceC(type): "interfaceC: \(type.value)"
        case let .unknown(type): "unknown: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "interfaceC: FOUR")
}

@Test
func testClassI_SealedInterfaceD() throws {
    let value = createClassI_SealedInterfaceA()
    var result = switch value.sealedType() {
        case let .sealedInterfaceD(.classI(type)): "classI: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "classI: ClassI")
    result = switch createClassI_SealedInterfaceD().sealedType() {
        case let .classI(type): "classI: \(type.value)"
    }
    try #require(result == "classI: ClassI")
}

@Test
func testIndirectSubclassReportsTheOpenInheritorsCase() throws {
    func describe(_ value: SealedClassA) -> String {
        switch value.sealedType() {
            case let .classJ(type): "classJ: \(type.value)"
            default: "default: \(value)"
        }
    }
    try #require(describe(createClassJ_SealedClassA()) == "classJ: ClassJ")
    // `ClassK` is not a direct inheritor, so it has no case of its own.
    try #require(describe(createClassK_SealedClassA()) == "classJ: ClassK")
}
