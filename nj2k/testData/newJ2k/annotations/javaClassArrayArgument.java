@interface Ann {
    Class<?>[] value();
}

@Ann({String.class, Object.class})
class C {
}

@Ann({})
class D {
}