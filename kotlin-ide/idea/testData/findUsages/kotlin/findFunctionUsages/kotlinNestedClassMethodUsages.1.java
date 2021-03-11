package b;

import a.Outer;

class X extends Outer.Inner {
    {
        new Outer.Inner().foo();
    }
}
