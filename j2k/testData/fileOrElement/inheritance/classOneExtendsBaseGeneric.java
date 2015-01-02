//file
class Base<T> {
  Base(T name) { }
}

class One<T, K> extends Base<T> {
  private K mySecond;

  One(T name, K second) {
    super(name)
    mySecond = second;
  }
}