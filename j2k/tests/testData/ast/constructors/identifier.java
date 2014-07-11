public class Identifier {
  private final String myName;
  private boolean myHasDollar;
  private boolean myNullable = true;

  public Identifier(String name) {
    myName = name;
  }

  public Identifier(String name, boolean isNullable) {
    myName = name;
    myNullable = isNullable;
  }

  public Identifier(String name, boolean hasDollar, boolean isNullable) {
    myName = name;
    myHasDollar = hasDollar;
    myNullable = isNullable;
  }

  @Override
  public String getName() {
    return myName;
  }
}

public class User {
  public static void main() {
     Identifier i1 = new Identifier("name", false, true);
     Identifier i2 = new Identifier("name", false);
     Identifier i3 = new Identifier("name");
  }
}