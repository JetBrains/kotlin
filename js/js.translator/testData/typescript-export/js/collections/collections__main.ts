import provideList = JS_TESTS.provideList;
import consumeList = JS_TESTS.consumeList;
import provideMutableList = JS_TESTS.provideMutableList;
import consumeMutableList = JS_TESTS.consumeMutableList;
import provideMutableSet = JS_TESTS.provideMutableSet;
import provideSet = JS_TESTS.provideSet;
import consumeSet = JS_TESTS.consumeSet;
import consumeMutableSet = JS_TESTS.consumeMutableSet;
import provideMap = JS_TESTS.provideMap;
import consumeMap = JS_TESTS.consumeMap;
import provideMutableMap = JS_TESTS.provideMutableMap;
import consumeMutableMap = JS_TESTS.consumeMutableMap;
import KtList = JS_TESTS.kotlin.collections.KtList;
import KtMutableList = JS_TESTS.kotlin.collections.KtMutableList;
import KtSet = JS_TESTS.kotlin.collections.KtSet;
import KtMutableSet = JS_TESTS.kotlin.collections.KtMutableSet;
import KtMutableMap = JS_TESTS.kotlin.collections.KtMutableMap;
import KtMap = JS_TESTS.kotlin.collections.KtMap;

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw message
    }
}

function assertThrow(fn: () => void, message: string) {
    try {
       fn();
       throw message;
    } catch (e) {}
}

function box(): string {
    testImmutableList()
    testImmutableSet()
    testImmutableMap()

    testMutableList()
    testMutableSet()
    testMutableMap()

    return "OK"
}

function testImmutableList() {
    const list = provideList()
    const listReadonlyArrayView = list.asJsReadonlyArrayView()

    assert(listReadonlyArrayView[0] == 1, "Problem with accessing of element in immutable list readonly array view")
    assert(listReadonlyArrayView["0"] == 1, "Problem with accessing of element in immutable list readonly array view by string")
    assert(listReadonlyArrayView.map(x => x + 1).join("") == "234", "Problem with immutable list readonly array view")
    assert(consumeList(list), "Problem with consumption of a Kotlin list")
    assertThrow(() => { (listReadonlyArrayView as Array<number>)[1] = 4 }, "Immutable list readonly array view have ability to mutate the list by direct set")
    assertThrow(() => { (listReadonlyArrayView as Array<number>).push(4) }, "Immutable list readonly array view have ability to mutate the list by 'push'")
    assertThrow(() => { (listReadonlyArrayView as Array<number>).pop() }, "Immutable list readonly array view have ability to mutate the list by 'pop'")
    // @ts-expect-error
    assertThrow(() => { listReadonlyArrayView["foo"] }, "Immutable list getting a random index from its readonly array view")

    assert(consumeList(KtList.fromJsArray([1, 2, 3])), "Problem with array to list conversion for Kotlin list")

    assert(Array.isArray(listReadonlyArrayView), "Problem with the readonly array view of immutable list in Array.isArray check")
}

function testMutableList() {
    const mutableList = provideMutableList()
    const mutableListReadonlyArrayView = mutableList.asJsReadonlyArrayView()

    assert(mutableListReadonlyArrayView[0] == 4, "Problem with accessing of element in mutable list readonly array view")
    assert(mutableListReadonlyArrayView["0"] == 4, "Problem with accessing of element in immutable list readonly array view by string")
    assert(mutableListReadonlyArrayView.map(x => x + 1).join("") == "567", "Problem with mutable list readonly array view")
    assert(!consumeList(mutableList), "Problem with consumption of a Kotlin mutable list as a list")
    assert(consumeMutableList(mutableList), "Problem with consumption of a Kotlin mutable list as a mutable list")
    assert(mutableListReadonlyArrayView.map(x => x + 1).join("") == "5678", "Problem with mutable list readonly array view after original list is mutated")
    assertThrow(() => { (mutableListReadonlyArrayView as Array<number>)[1] = 4 }, "Mutable list readonly array view have ability to mutate the list by direct set")
    assertThrow(() => { (mutableListReadonlyArrayView as Array<number>).push(4) }, "Mutable list readonly array view have ability to mutate the list by 'push'")
    assertThrow(() => { (mutableListReadonlyArrayView as Array<number>).pop() }, "Mutable list readonly array view have ability to mutate the list by 'pop'")
    // @ts-expect-error
    assertThrow(() => { mutableListReadonlyArrayView["foo"] }, "Immutable list getting a random index from its readonly array view")

    assert(Array.isArray(mutableListReadonlyArrayView), "Problem with the readonly array view of mutable list in Array.isArray check")

    const mutableListArrayView = mutableList.asJsArrayView()
    mutableListArrayView.pop()

    assert(mutableListArrayView[0] == 4, "Problem with accessing of element in mutable list array view")
    assert(mutableListArrayView["0"] == 4, "Problem with accessing of element in mutable list array view by string")
    assert(mutableListArrayView.map(x => x + 1).join("") == "567", "Problem with mutable list array view")
    assert(consumeMutableList(mutableList), "Problem with consumption of a Kotlin mutable list as a mutable list")
    assert(mutableListArrayView.map(x => x + 1).join("") == "5678", "Problem with mutable list array view after original list is mutated")
    // @ts-expect-error
    assertThrow(() => { mutableListArrayView["foo"] = 4 }, "Mutable list setting a random index in its array view")

    assert(Array.isArray(mutableListArrayView), "Problem with the array view of mutable list in Array.isArray check")

    mutableListArrayView.shift()
    mutableListArrayView.unshift(9)

    assert(mutableListArrayView.map(x => x + 1).join("") == "10678", "Problem with mutable list array view after the view is mutated")

    mutableListArrayView.sort()

    assert(mutableListArrayView.map(x => x + 1).join("") == "67810", "Problem with mutable list array view after the view is mutated")

    mutableListArrayView.push(3)

    assert(mutableListArrayView.map(x => x + 1).join("") == "678104", "Problem with mutable list array view after the view is mutated")

    mutableListArrayView[3] = 4

    assert(mutableListArrayView.map(x => x + 1).join("") == "67854", "Problem with mutable list array view after the view is mutated")

    mutableListArrayView["3"] = 6

    assert(mutableListArrayView.map(x => x + 1).join("") == "67874", "Problem with mutable list array view after the view is mutated")

    mutableListArrayView.length = 3

    assert(mutableListArrayView.map(x => x + 1).join("") == "678", "Problem with mutable list array view after the view is mutated after size decreased")
    assert(mutableListReadonlyArrayView.map(x => x + 1).join("") == "678", "Problem with mutable list readonly array view after size decreased")

    assertThrow(() => { mutableListArrayView.length = 5 }, "Mutable list view size increasing works, but should not")

    assert(consumeList(KtMutableList.fromJsArray([1, 2, 3])), "Problem with array to list conversion for Kotlin mutable list")
    assert(consumeMutableList(KtMutableList.fromJsArray([4, 5, 6])), "Problem with array to mutable list conversion for Kotlin mutable list")
}

function testImmutableSet() {
    const set = provideSet()
    const setReadonlyView = set.asJsReadonlySetView();

    assert(setReadonlyView.has(1), "Problem with accessing element of readonly view")
    assert(joinIterator(setReadonlyView.keys()) == "123", "Unexpected keys() in set readonly view")
    assert(joinIterator(setReadonlyView.values()) == "123", "Unexpected values() in set readonly view")
    assert(joinIterator(setReadonlyView.entries()) == "1,12,23,3", "Unexpected entries() in set readonly view")
    assert(Array.from(setReadonlyView.keys()).toString() == "", "Unexpected array created from keys() in set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(setReadonlyView.values()).toString() == "", "Unexpected array created from values() in set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(setReadonlyView.entries()).toString() == "", "Unexpected array created from entries() in set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(setReadonlyView) == "123", "Problem with readonly view iterator")
    assert(consumeSet(set), "Problem with consumption of a Kotlin set")
    assertThrow(() => { (setReadonlyView as Set<number>).add(4) }, "Set readonly view have ability to mutate the set by 'add'")
    assertThrow(() => { (setReadonlyView as Set<number>).delete(4) }, "Set readonly view have ability to mutate the set by 'delete'")
    assertThrow(() => { (setReadonlyView as Set<number>).clear() }, "Set readonly view have ability to mutate the set by 'clear'")

    assert(consumeSet(KtSet.fromJsSet(new Set([1, 2, 3]))), "Problem with set to set conversion for Kotlin set")
}

function testMutableSet() {
    const mutableSet = provideMutableSet()
    const mutableSetReadonlyView = mutableSet.asJsReadonlySetView()

    assert(mutableSetReadonlyView.has(4), "Problem with accessing element of mutable set readonly view")
    assert(joinIterator(mutableSetReadonlyView.keys()) == "456", "Unexpected keys() in mutable set readonly view")
    assert(joinIterator(mutableSetReadonlyView.values()) == "456", "Unexpected values() in mutable set readonly view")
    assert(joinIterator(mutableSetReadonlyView.entries()) == "4,45,56,6", "Unexpected entries() in mutable set readonly view")
    assert(Array.from(mutableSetReadonlyView.keys()).toString() == "", "Unexpected array created from keys() in mutable set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableSetReadonlyView.values()).toString() == "", "Unexpected array created from values() in mutable set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableSetReadonlyView.entries()).toString() == "", "Unexpected array created from entries() in mutable set readonly view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(mutableSetReadonlyView) == "456", "Problem with mutable set readonly view iterator")
    assert(!consumeSet(mutableSet), "Problem with consumption of a Kotlin mutable set as a set")
    assert(consumeMutableSet(mutableSet), "Problem with consumption of a Kotlin mutable set as a mutable set")
    assert(joinSetOrMap(mutableSetReadonlyView) == "4567", "Problem with mutable set readonly view after original set is mutated")
    assertThrow(() => { (mutableSetReadonlyView as Set<number>).add(4) }, "Mutable set readonly view have ability to mutate the set by 'add'")
    assertThrow(() => { (mutableSetReadonlyView as Set<number>).delete(4) }, "Mutable set readonly view have ability to mutate the set by 'delete'")
    assertThrow(() => { (mutableSetReadonlyView as Set<number>).clear() }, "Mutable set readonly view have ability to mutate the set by 'clear'")

    const mutableSetView = mutableSet.asJsSetView()

    mutableSetView.delete(7)

    assert(mutableSetView.has(4), "Problem with accessing element of mutable set view")
    assert(joinIterator(mutableSetView.keys()) == "456", "Unexpected keys() in mutable set view")
    assert(joinIterator(mutableSetView.values()) == "456", "Unexpected values() in mutable set view")
    assert(joinIterator(mutableSetView.entries()) == "4,45,56,6", "Unexpected entries() in mutable set view")
    assert(Array.from(mutableSetView.keys()).toString() == "", "Unexpected array created from keys() in mutable set view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableSetView.values()).toString() == "", "Unexpected array created from values() in mutable set view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableSetView.entries()).toString() == "", "Unexpected array created from entries() in mutable set view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(mutableSetView) == "456", "Problem with mutable set view")
    assert(consumeMutableSet(mutableSet), "Problem with consumption of a Kotlin mutable set as a mutable set")
    assert(joinSetOrMap(mutableSetView) == "4567", "Problem with mutable set view after original set is mutated")

    mutableSetView.add(8)

    assert(joinSetOrMap(mutableSetView) == "45678", "Problem with mutable set view after the view is mutated")

    mutableSetView.clear()

    assert(joinSetOrMap(mutableSetView) == "", "Problem with mutable set view after the view is mutated")

    assert(consumeSet(KtMutableSet.fromJsSet(new Set([1, 2, 3]))), "Problem with set to set conversion for Kotlin mutable set")
    assert(consumeMutableSet(KtMutableSet.fromJsSet(new Set([4, 5, 6]))), "Problem with set to mutable set conversion for Kotlin mutable set")
}

function testImmutableMap() {
    const map = provideMap()
    const mapReadonlyView = map.asJsReadonlyMapView()

    assert(mapReadonlyView.has("a"), "Problem with accessing element in map readonly view")
    assert(mapReadonlyView.get("a") == 1, "Problem with accessing element in map readonly view")
    assert(joinIterator(mapReadonlyView.keys()) == "abc", "Unexpected keys() in map readonly view")
    assert(joinIterator(mapReadonlyView.values()) == "123", "Unexpected values() in map readonly view")
    assert(joinIterator(mapReadonlyView.entries()) == "a,1b,2c,3", "Unexpected entries() in map readonly view")
    assert(Array.from(mapReadonlyView.keys()).toString() == "", "Unexpected array created from keys() in map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mapReadonlyView.values()).toString() == "", "Unexpected array created from values() in map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mapReadonlyView.entries()).toString() == "", "Unexpected array created from entries() in map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(mapReadonlyView) == "[a:1][b:2][c:3]", "Problem with map readonly view iterator")
    assert(consumeMap(map), "Problem with consumption of a Kotlin map")
    assertThrow(() => { (mapReadonlyView as Map<string, number>).set("d", 4) }, "Map readonly view have ability to mutate the map by 'set'")
    assertThrow(() => { (mapReadonlyView as Map<string, number>).delete("a") }, "Map readonly view have ability to mutate the map by 'delete'")
    assertThrow(() => { (mapReadonlyView as Map<string, number>).clear() }, "Map readonly view have ability to mutate the map by 'clear'")

    assert(consumeMap(KtMap.fromJsMap(new Map([["a", 1], ["b", 2], ["c", 3]]))), "Problem with map to map conversion for Kotlin map")
}

function testMutableMap() {
    const mutableMap = provideMutableMap()
    const mutableMapReadonlyView = mutableMap.asJsReadonlyMapView()

    assert(mutableMapReadonlyView.has("d"), "Problem with accessing element in mutable map readonly view")
    assert(mutableMapReadonlyView.get("d") == 4, "Problem with accessing element in mutable map readonly view")
    assert(joinIterator(mutableMapReadonlyView.keys()) == "def", "Unexpected keys() in mutable map readonly view")
    assert(joinIterator(mutableMapReadonlyView.values()) == "456", "Unexpected values() in mutable map readonly view")
    assert(joinIterator(mutableMapReadonlyView.entries()) == "d,4e,5f,6", "Unexpected entries() in mutable map readonly view")
    assert(Array.from(mutableMapReadonlyView.keys()).toString() == "", "Unexpected array created from keys() in mutable map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableMapReadonlyView.values()).toString() == "", "Unexpected array created from values() in mutable map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableMapReadonlyView.entries()).toString() == "", "Unexpected array created from entries() in mutable map readonly view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(mutableMapReadonlyView) == "[d:4][e:5][f:6]", "Problem with mutable map readonly view")
    assert(!consumeMap(mutableMap), "Problem with consumption of a Kotlin mutable map as a map")
    assert(consumeMutableMap(mutableMap), "Problem with consumption of a Kotlin mutable map as a mutable map")
    assert(joinSetOrMap(mutableMapReadonlyView) == "[d:4][e:5][f:6][g:7]", "Problem with mutable map readonly view after original map is mutated")
    assertThrow(() => { (mutableMapReadonlyView as Map<string, number>).set("d", 4) }, "Mutable map readonly view have ability to mutate the map by 'set'")
    assertThrow(() => { (mutableMapReadonlyView as Map<string, number>).delete("a") }, "Mutable map readonly view have ability to mutate the map by 'delete'")
    assertThrow(() => { (mutableMapReadonlyView as Map<string, number>).clear() }, "Mutable map readonly view have ability to mutate the map by 'clear'")

    const mutableMapView = mutableMap.asJsMapView()

    mutableMapView.delete("g")

    assert(mutableMapView.has("d"), "Problem with accessing element in mutable map view")
    assert(mutableMapView.get("d") == 4, "Problem with accessing element in mutable map view")
    assert(joinIterator(mutableMapView.keys()) == "def", "Unexpected keys() in mutable map view")
    assert(joinIterator(mutableMapView.values()) == "456", "Unexpected values() in mutable map view")
    assert(joinIterator(mutableMapView.entries()) == "d,4e,5f,6", "Unexpected entries() in mutable map view")
    assert(Array.from(mutableMapView.keys()).toString() == "", "Unexpected array created from keys() in mutable map view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableMapView.values()).toString() == "", "Unexpected array created from values() in mutable map view") // FIXME(KT-69928): The array should not be empty
    assert(Array.from(mutableMapView.entries()).toString() == "", "Unexpected array created from entries() in mutable map view") // FIXME(KT-69928): The array should not be empty
    assert(joinSetOrMap(mutableMapView) == "[d:4][e:5][f:6]", "Problem with mutable map view")
    assert(consumeMutableMap(mutableMap), "Problem with consumption of a Kotlin mutable map as a mutable map")
    assert(joinSetOrMap(mutableMapView) == "[d:4][e:5][f:6][g:7]", "Problem with mutable map view after original map is mutated")

    mutableMapView.set("h", 8)

    assert(joinSetOrMap(mutableMapView) == "[d:4][e:5][f:6][g:7][h:8]", "Problem with mutable map view after the view is mutated")

    mutableMapView.clear()

    assert(joinSetOrMap(mutableMapView) == "", "Problem with mutable map view after the view is mutated")

    assert(consumeMap(KtMutableMap.fromJsMap(new Map([["a", 1], ["b", 2], ["c", 3]]))), "Problem with map to map conversion for Kotlin mutable map")
    assert(consumeMutableMap(KtMutableMap.fromJsMap(new Map([["d", 4], ["e", 5], ["f", 6]]))), "Problem with map to mutable map conversion for Kotlin mutable map")
}

function joinSetOrMap(setOrMap: ReadonlySet<number> | ReadonlyMap<string, number>): string {
    let result = ""

    if (setOrMap instanceof Set) {
        setOrMap.forEach((a: any) => {
            result += a
        });
    } else {
        setOrMap.forEach((key: any, value: any) => {
            result += `[${key}:${value}]`
        });
    }

    return result
}

function joinIterator<T>(iterator: Iterator<T>): string {
    let result = ""
    let value = iterator.next()
    while (!value.done) {
        result += value.value
        value = iterator.next()
    }
    return result
}
