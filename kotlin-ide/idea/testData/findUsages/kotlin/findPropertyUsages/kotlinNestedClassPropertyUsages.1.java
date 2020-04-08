package b;

import a.Outer;

class X extends Outer.Inner {
    {
        int n = new Outer.Inner().getFoo();
        new Outer.Inner().setFoo(2);
    }
}
