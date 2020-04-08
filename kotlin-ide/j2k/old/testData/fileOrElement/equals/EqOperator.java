interface I{}

final class C{}

class O{}

final class E {
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}

class B {
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}

final class BB extends B {}

enum EE {
    A, B, C
}

class X {
    void foo(I i1, I i2, String s1, String s2, C c1, C c2, int i, O o1, O o2, E e1, E e2, BB bb1, BB bb2, int[] arr1, int[] arr2, EE ee1, EE ee2) {
        if (i1 == i2) return;
        if (s1 == s2) return;
        if (c1 == c2) return;
        if (i1 == null) return;
        if (null == i2) return;
        if (i == 0) return;
        if (o1 == o2) return;
        if (e1 == e2) return;
        if (bb1 == bb2) return;
        if (arr1 == arr2) return;
        if (ee1 == ee2 || ee1 == null) return;

        if (s1 != s2) return;
        if (c1 != c2) return;
    }
}
