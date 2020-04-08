package test;

import a.b.c.Outer;

class Test {
    public static void main(Outer o) {
        Outer oo = o.o();
        Outer.Inner oi = o.i();
        Outer.Nested on = o.n();
        Outer.Nested.NI oni = o.NI();
        Outer.Nested.NN onn = o.NN();
        Outer.Inner.II oii = o.II();
    }
}