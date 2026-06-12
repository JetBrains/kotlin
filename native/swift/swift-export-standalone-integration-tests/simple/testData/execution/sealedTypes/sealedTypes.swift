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
func testClassH_SealedClassA() throws {
    let value = createClassH_SealedClassA()
    let result = switch value.sealedType() {
        case let .unknown(type): "unknown: \(type.value)"
        default: "default: \(value)"
    }
    try #require(result == "unknown: ClassH")
}
