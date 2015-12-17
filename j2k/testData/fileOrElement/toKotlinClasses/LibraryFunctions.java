package demo;

import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.LazyThreadSafetyMode;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import java.util.ArrayList;
import java.util.List;

public class TestJava {
  public static void main(String[] args) {
    List<String> x = new ArrayList<String>();
    CollectionsKt.filter(x, new Function1<String, Boolean>() {
      @Override
      public Boolean invoke(String o) {
        return o.equals("a");
      }
    });
    Lazy<String> lazy = LazyKt.lazy(LazyThreadSafetyMode.NONE, new Function0<String>() {
      @Override
      public String invoke() {
        return "aaa";
      }
    });
  }

  public void f(Function1<String, Unit> result) {
    result.invoke("a")
  }
}
