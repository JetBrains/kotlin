@kotlin.contracts.ExperimentalContracts  // for tests in compiler/testData/codegen/box/contracts/
@kotlinx.cinterop.ExperimentalForeignApi // for tests in native/native.tests/testData/codegen/cinterop/
@kotlin.ExperimentalStdlibApi            // for test native/native.tests/testData/codegen/fileCheck/bce.kt
@kotlin.test.Test
fun runTest() {
    val result = box()
    kotlin.test.assertEquals("OK", result, "Test failed with: $result")
}
