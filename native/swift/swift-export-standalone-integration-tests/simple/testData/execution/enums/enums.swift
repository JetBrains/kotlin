import Enum
import KotlinRuntime
import Testing

@Test
func testEnums() throws {
    var en = Enum.a
    try #require(en.print() == "1 - str")
    en.i = 3
    try #require(en.print() == "3 - str")
    try #require(Enum.a.print() == "3 - str")
    try #require(Enum.a.description == "a")

    try #require(Enum.b.print() == "rts - 5")
    try #require(Enum("b")?.print() == "rts - 5")
    try #require(Enum.b.rawValue == 1)

    switch en {
    case .a: break;
    default: try #require(Bool(false), "switch over kotlin enum class should work")
    }

    try #require(Enum.allCases == [Enum.a, Enum.b])
    try #require(EmptyEnum.allCases.count == 0)

    try #require(consumeEnumAsAny(arg: Enum.a) == 3)
    try #require(consumeEnumAsAny(arg: Enum.b) == 7)
}

func consumeEnumAsAny(arg: Any) throws -> Int {
    switch arg {
    case Enum.a: return 3;
    case Enum.b: return 7;
    default: try #require(Bool(false), "switch over kotlin enum class should work")
    }

    return -1
}