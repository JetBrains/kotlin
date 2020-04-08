package b;

import a.Outer;

public class X extends Outer.A {
    Outer.A next = new Outer.A();
}