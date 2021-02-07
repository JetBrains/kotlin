package test

public class InheritMethodsDifferentReturnTypesGeneric {
    public interface Super1<F, B> {
        public fun foo(): F?
        public fun bar(): B?
    }

    public interface Super2<FF, BB> {
        public fun foo(): FF?
        public fun bar(): BB?
    }

    public interface Sub: Super1<String, CharSequence>, Super2<CharSequence, String> {
    }
}
