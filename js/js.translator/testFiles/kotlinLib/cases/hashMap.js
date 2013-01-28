/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var map = Kotlin.$new(Kotlin.HashMap)()

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
