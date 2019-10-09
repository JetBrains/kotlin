package test;

public class Some {
    @SomeAnnotation(some = "Foo", same = 0)
    public void foo() {

    }

    @SomeAnnotation(some = {"Bar", "Buz"}, same = {1, 2})
    public void bar() {

    }
}