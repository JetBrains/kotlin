public class Sample {
    public void foo() {
        char c = 'a';
        byte b = 0;
        short s = 0;
        bar(c);
        bar(b);
        bar(s);
    }

    public void bar(int i) {}
}