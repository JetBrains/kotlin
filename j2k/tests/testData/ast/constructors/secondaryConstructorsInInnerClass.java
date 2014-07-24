//file
class Outer {
    private class Inner1 {
        public Inner1(){}

        public Inner1(int a) {
            this();
        }

        protected Inner1(char c) {
            this();
        }

        private Inner1(boolean b) {
            this();
        }
    }

    protected class Inner2 {
        public Inner2(){}

        public Inner2(int a) {
            this();
        }

        protected Inner2(char c) {
            this();
        }

        private Inner2(boolean b) {
            this();
        }

    }

    class Inner3 {
        public Inner3(){}

        public Inner3(int a) {
            this();
        }

        protected Inner3(char c) {
            this();
        }

        private Inner3(boolean b) {
            this();
        }
    }

    public class Inner4 {
        public Inner4(){}

        public Inner4(int a) {
            this();
        }

        protected Inner4(char c) {
            this();
        }

        private Inner4(boolean b) {
            this();
        }
    }

    void foo() {
        Inner1 inner1 = new Inner1(1);
        Inner2 inner2 = new Inner2(2);
        Inner3 inner3 = new Inner3(3);
        Inner4 inner4 = new Inner4(4);
    }
}
