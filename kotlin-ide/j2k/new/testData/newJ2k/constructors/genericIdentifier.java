public class Identifier<T> {
  private final T myName;
  private boolean myHasDollar;
  private boolean myNullable = true;

  public Identifier(T name) {
    myName = name;
  }

  public Identifier(T name, boolean isNullable) {
    myName = name;
    myNullable = isNullable;
  }

  public Identifier(T name, boolean hasDollar, boolean isNullable) {
    myName = name;
    myHasDollar = hasDollar;
    myNullable = isNullable;
  }

  @Override
  public T getName() {
    return myName;
  }
}

public class User {
  public static void main() {
     Identifier<?> i1 = new Identifier<String>("name", false, true);
     Identifier i2 = new Identifier<String>("name", false);
     Identifier i3 = new Identifier<String>("name");
  }
}