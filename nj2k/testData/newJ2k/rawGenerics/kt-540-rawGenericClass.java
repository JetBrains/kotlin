//file
package demo;

class Collection<E> {
  Collection(E e) {
    System.out.println(e);
  }
}

class Test {
  void main() {
    Collection raw1 = new Collection(1);
    Collection raw2 = new Collection<Integer>(1);
    Collection raw3 = new Collection<String>("1");
  }
}