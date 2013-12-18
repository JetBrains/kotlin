//file
class Base {
  Base(String name) {
  }
}

class One extends Base {
  private String mySecond;

  One(String name, String second) {
    super(name);
    mySecond = second;
  }
}