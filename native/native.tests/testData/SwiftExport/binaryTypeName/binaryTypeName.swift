import base
import dep
import ObjectiveC

func testBinaryNames() throws {
    try assertSame(
        actual: objc_getClass("SEExportedKotlinPackages_base_ClassInBase") as! base.ClassInBase.Type,
        expected: base.ClassInBase.self
    )
    try assertSame(
        actual: objc_getClass("SEdep_ClassInDep") as! dep.ClassInDep.Type,
        expected: dep.ClassInDep.self
    )
}

class BinaryTypeNameTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testBinaryNames", method: withAutorelease(testBinaryNames)),
        ]
    }
}