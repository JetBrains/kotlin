import KtList = JS_TESTS.kotlin.collections.KtList;
import KtMutableList = JS_TESTS.kotlin.collections.KtMutableList;

function assertEquals(expected: any, actual: any, message: string) {
    if (expected !== actual) {
        throw `Expected ${expected}, got ${actual}: ${message}`
    }
}

function box(): string {
    const list = KtList.fromJsArray([1, 2, 3]);
    assertEquals(list.toString(), "[1, 2, 3]", "KtList.fromJsArray")
    assertEquals(list.asJsReadonlyArrayView().toString(), "1,2,3", "KtList.asJsReadonlyArrayView")

    const mutableList = KtMutableList.fromJsArray([1, 2, 3]);
    assertEquals(mutableList.toString(), "[1, 2, 3]", "KtMutableList.fromJsArray")
    assertEquals(mutableList.asJsReadonlyArrayView().toString(), "1,2,3", "KtMutableList.asJsReadonlyArrayView")
    assertEquals(mutableList.asJsArrayView().toString(), "1,2,3", "KtMutableList.asJsArrayView")

    return "OK"
}
