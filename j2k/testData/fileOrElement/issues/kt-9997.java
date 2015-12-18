class A {
    public void equals() {
        int i = 1;
        byte b = 1;
        short s = 1;
        long l = 1;
        double d = 1.0;
        float f = 1.0f;
        char c = 1;

        t(i == i);
        t(i == b);
        t(i == s);
        t(i == l);
        t(i == d);
        t(i == f);
        t(i == c);

        t(b == i);
        t(b == b);
        t(b == s);
        t(b == l);
        t(b == d);
        t(b == f);
        t(b == c);

        t(s == i);
        t(s == b);
        t(s == s);
        t(s == l);
        t(s == d);
        t(s == f);
        t(s == c);

        t(l == i);
        t(l == b);
        t(l == s);
        t(l == l);
        t(l == d);
        t(l == f);
        t(l == c);

        t(d == i);
        t(d == b);
        t(d == s);
        t(d == l);
        t(d == d);
        t(d == f);
        t(d == c);

        t(f == i);
        t(f == b);
        t(f == s);
        t(f == l);
        t(f == d);
        t(f == f);
        t(f == c);

        t(c == i);
        t(c == b);
        t(c == s);
        t(c == l);
        t(c == d);
        t(c == f);
        t(c == c);

        t(i != d);
    }

    public void compare() {
        int i = 1;
        byte b = 1;
        short s = 1;
        long l = 1;
        double d = 1.0;
        float f = 1.0f;
        char c = 1;

        t(i > i);
        t(i > b);
        t(i > s);
        t(i > l);
        t(i > d);
        t(i > f);
        t(i > c);

        t(b > i);
        t(b > b);
        t(b > s);
        t(b > l);
        t(b > d);
        t(b > f);
        t(b > c);

        t(s > i);
        t(s > b);
        t(s > s);
        t(s > l);
        t(s > d);
        t(s > f);
        t(s > c);

        t(l > i);
        t(l > b);
        t(l > s);
        t(l > l);
        t(l > d);
        t(l > f);
        t(l > c);

        t(d > i);
        t(d > b);
        t(d > s);
        t(d > l);
        t(d > d);
        t(d > f);
        t(d > c);

        t(f > i);
        t(f > b);
        t(f > s);
        t(f > l);
        t(f > d);
        t(f > f);
        t(f > c);

        t(c > i);
        t(c > b);
        t(c > s);
        t(c > l);
        t(c > d);
        t(c > f);
        t(c > c);
    }

    private void t(boolean b) {
    }
}