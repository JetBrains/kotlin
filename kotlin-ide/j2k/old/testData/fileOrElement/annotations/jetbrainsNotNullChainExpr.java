// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
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