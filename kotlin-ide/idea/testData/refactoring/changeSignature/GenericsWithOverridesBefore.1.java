interface J<A, B> extends T<A, B> {
    @Override
    <C> int foofoofoo(A a, B b, C c);
}

abstract class J1<X, Y> implements J<U<X>, U<Y>> {
    @Override
    public <C> int foofoofoo(U<X> xu, U<Y> yu, C c) {
        throw new UnsupportedOperationException();
    }
}

abstract class J2<X> extends J1<X, String> {
    @Override
    public <C> int foofoofoo(U<X> xu, U<String> stringU, C c) {
        throw new UnsupportedOperationException();
    }
}

class J3 extends J2<Object> {
    @Override
    public <D> int foofoofoo(U<Object> objectU, U<String> stringU, D c) {
        throw new UnsupportedOperationException();
    }
}