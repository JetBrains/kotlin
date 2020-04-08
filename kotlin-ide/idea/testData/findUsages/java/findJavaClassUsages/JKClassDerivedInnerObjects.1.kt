class Outer {
    public interface T: A

    public object O1: A()

    class Inner {
        public object O2: T
    }
}

