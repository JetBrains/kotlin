//file
class Test {
    public void operationsWithChar() {
        char c = 1;
        int i = 1;

        b(i > c);
        b(i >= c);
        b(i < c);
        b(i <= c);

        b(c > i);
        b(c >= i);
        b(c < i);
        b(c <= i);

        b(c == i);
        b(c != i);
        b(i == c);
        b(i != c);

        i(i + c);
        i(i - c);
        i(i / c);
        i(i * c);
        i(i % c);
        i(i | c);
        i(i & c);
        i(i << c);
        i(i >> c);

        i(c + i);
        i(c - i);
        i(c / i);
        i(c * i);
        i(c % i);
        i(c | i);
        i(c & i);
        i(c << i);
        i(c >> i);
    }

    public void b(boolean b) {}
    public void i(int i) {}
}