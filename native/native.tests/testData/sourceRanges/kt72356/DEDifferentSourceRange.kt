class D { @Something fun bar() {} }

class E : C() // E has a fake override of foo(), which has annotation with const param having ANOTHER source range as @Something, in ANOTHER source file
