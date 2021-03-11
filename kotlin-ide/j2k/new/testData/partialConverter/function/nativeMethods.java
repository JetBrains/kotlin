public class Foo {
    private native final void nativeMethod();

    public native final int <caret>getBar();
    public native final void setBar(int bar);
}
