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

    return "OK"
}

function testImmutableList() {
    const list = provideList()
    const listArrayView = list.asJsArrayView()

    assert(listArrayView.map(x => x + 1).join("") == "234", "Problem with immutable list array view")
    assert(consumeList(list), "Problem with consumption of a Kotlin list")
    assertThrow(() => { (listArrayView as Array<number>)[1] = 4 }, "Immutable list array view have ability to mutate the list by direct set")
    assertThrow(() => { (listArrayView as Array<number>).push(4) }, "Immutable list array view have ability to mutate the list by 'push'")
    assertThrow(() => { (listArrayView as Array<number>).pop() }, "Immutable list array view have ability to mutate the list by 'pop'")
}

function testMutableList() {
    const mutableList = provideMutableList()
    const mutableListView = mutableList.asJsArrayView()

    assert(mutableListView.map(x => x + 1).join("") == "567", "Problem with mutable list array view")
    assert(!consumeList(mutableList), "Problem with consumption of a Kotlin mutable list as a list")
    assert(consumeMutableList(mutableList), "Problem with consumption of a Kotlin mutable list as a mutable list")
    assert(mutableListView.map(x => x + 1).join("") == "5678", "Problem with mutable list array view after original list is mutated")
}

function testImmutableSet() {
    const set = provideSet()
    const setView = set.asJsSetView();

    assert(joinSetOrMap(setView) == "123", "Problem with immutable set view")
    assert(consumeSet(set), "Problem with consumption of a Kotlin set")
    assertThrow(() => { (setView as Set<number>).add(4) }, "Immutable set view have ability to mutate the set by 'add'")
    assertThrow(() => { (setView as Set<number>).delete(4) }, "Immutable set view have ability to mutate the set by 'delete'")
    assertThrow(() => { (setView as Set<number>).clear() }, "Immutable set view have ability to mutate the set by 'clear'")
}

function testMutableSet() {
    const mutableSet = provideMutableSet()
    const mutableSetView = mutableSet.asJsSetView()

    assert(joinSetOrMap(mutableSetView) == "456", "Problem with mutable set view")
    assert(!consumeSet(mutableSet), "Problem with consumption of a Kotlin mutable set as a set")
    assert(consumeMutableSet(mutableSet), "Problem with consumption of a Kotlin mutable set as a mutable set")
    assert(joinSetOrMap(mutableSetView) == "4567", "Problem with mutable set view after original set is mutated")
}

function testImmutableMap() {
    const map = provideMap()
    const mapView = map.asJsMapView()

    assert(joinSetOrMap(mapView) == "[a:1][b:2][c:3]", "Problem with immutable map view")
    assert(consumeMap(map), "Problem with consumption of a Kotlin map")
    assertThrow(() => { (mapView as Map<string, number>).set("d", 4) }, "Immutable map view have ability to mutate the map by 'set'")
    assertThrow(() => { (mapView as Map<string, number>).delete("a") }, "Immutable map view have ability to mutate the map by 'delete'")
    assertThrow(() => { (mapView as Map<string, number>).clear() }, "Immutable map view have ability to mutate the map by 'clear'")
}

function testMutableMap() {
    const mutableMap = provideMutableMap()
    const mutableMapView = mutableMap.asJsMapView()

    assert(joinSetOrMap(mutableMapView) == "[d:4][e:5][f:6]", "Problem with mutable map view")
    assert(!consumeMap(mutableMap), "Problem with consumption of a Kotlin mutable map as a map")
    assert(consumeMutableMap(mutableMap), "Problem with consumption of a Kotlin mutable map as a mutable map")
    assert(joinSetOrMap(mutableMapView) == "[d:4][e:5][f:6][g:7]", "Problem with mutable map view after original map is mutated")
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