package demo;

enum MyEnum {
  RED(10),
  BLUE(20);

  private final int color;

  private MyEnum(int _color) {
    color = _color;
  }

  public int getColor() {
    return color;
  }
}