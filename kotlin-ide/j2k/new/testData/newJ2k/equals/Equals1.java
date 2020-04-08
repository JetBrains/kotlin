interface I {
}

class C{
    boolean foo1(I i1, I i2) {
        return i1.equals(i2)
    }

    boolean foo2(I i1, I i2) {
        return !i1.equals(i2)
    }
}