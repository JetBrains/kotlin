var map = new Kotlin.HashMap()

map.put(3, 4)
map.put(6, 3)

function test() {
    if (map.containsKey(4) || map.containsKey(5)) return false;
    if (map.containsKey({}) || map.containsKey(11)) return false;
    if (!map.containsKey(3) || !map.containsKey(6)) return false;
    var obj = map.get(3);
    if (obj !== 4) return false;
    obj = map.get(6);
    if (obj !== 3) return false;
    if (map.size() !== 2) return false;
    map.put(2, 3);
    if (map.size() !== 3) return false;
    if (!map.containsKey(2) || !map.containsKey(3) || !map.containsKey(6)) return false;
    if (!map.containsValue(4) || !map.containsValue(3)) return false;
    map.put(2, 1000);
    if (map.size() !== 3) return false;
    if (map.get(2)!== 1000) return false;


    return true;
}