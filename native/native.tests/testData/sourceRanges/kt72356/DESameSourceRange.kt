class D {         @Something fun bar() {} }

class E : C() // E has a fake override of foo(), which has annotation with const param having SAME source range as @Something, but in ANOTHER source file
