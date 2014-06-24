//file
// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
package test;

import org.jetbrains.annotations.NotNull;

class Foo {
   void execute() {}
}

class Bar {
  @NotNull
  Foo fooNotNull = new Foo();
  Foo fooNullable = null;
}

class Test {
  public void test(@NotNull Bar barNotNull, Bar barNullable) {
    barNotNull.fooNotNull.execute();
    barNotNull.fooNullable.execute();
    barNullable.fooNotNull.execute();
    barNullable.fooNullable.execute();
  }
}