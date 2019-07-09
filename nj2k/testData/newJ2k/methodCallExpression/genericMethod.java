//file
package demo;

class Map {
  <K, V> void put(K k, V v) {}
}

class U {
  void test() {
    Map m = new Map();
    m.<String, Integer>put(null, 10);
  }
}