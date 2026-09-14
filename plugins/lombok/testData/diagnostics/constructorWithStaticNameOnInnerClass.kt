// ISSUE: KT-89169
// A static factory has no enclosing instance to pass to `new Inner(...)`, so nothing is generated for an inner
// class: neither the factory, nor the constructor that would only exist for it to call. The default constructor
// stays hidden by `staticName`, the way it is on any class.

// FILE: Outer.java
import lombok.*;

public class Outer {
    @NoArgsConstructor(staticName = "of", force = true)
    public class InnerNoArgs {
        public int value;
    }

    @AllArgsConstructor(staticName = "of")
    public class InnerAllArgs {
        public int value;
    }

    @Data(staticConstructor = "of")
    public class InnerData {
        private final int value;
    }

    @Value(staticConstructor = "of")
    public class InnerValue {
        int value;
    }
}

// FILE: GenericOuter.java
import lombok.*;

public class GenericOuter<T> {
    @AllArgsConstructor(staticName = "of")
    public class Inner {
        public T value;
    }
}

// FILE: test.kt
fun test(outer: Outer, genericOuter: GenericOuter<String>) {
    Outer.InnerNoArgs.<!UNRESOLVED_REFERENCE!>of<!>()
    Outer.InnerAllArgs.<!UNRESOLVED_REFERENCE!>of<!>(1)
    Outer.InnerData.<!UNRESOLVED_REFERENCE!>of<!>(1)
    Outer.InnerValue.<!UNRESOLVED_REFERENCE!>of<!>(1)
    GenericOuter.Inner.<!UNRESOLVED_REFERENCE!>of<!>("a")

    outer.<!INVISIBLE_REFERENCE!>InnerNoArgs<!>()
    genericOuter.<!INVISIBLE_REFERENCE!>Inner<!>()
}
