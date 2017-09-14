package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) /*p:kotlin(Boolean)*/{
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(equals)*/== /*p:kotlin(Any)*/c
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(equals)*/!= /*p:kotlin(Any)*/c
    /*p:foo.bar(A) p:kotlin(Boolean)*/na /*c:foo.bar.A(equals)*/== /*p:foo.bar(A)*/a
    /*p:foo.bar(A) p:kotlin(Boolean)*/na /*c:foo.bar.A(equals)*/== /*p:kotlin(Nothing)*/null

    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo)*/> /*p:kotlin(Int)*/b
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo)*/< /*p:kotlin(Int)*/b
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo)*/>= /*p:kotlin(Int)*/b
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo)*/<= /*p:kotlin(Int)*/b

    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/> /*p:kotlin(Any)*/c
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/< /*p:kotlin(Any)*/c
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/>= /*p:kotlin(Any)*/c
    /*p:foo.bar(A) p:kotlin(Boolean)*/a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/<= /*p:kotlin(Any)*/c
}
