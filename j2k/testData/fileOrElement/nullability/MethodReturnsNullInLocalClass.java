class C {
    String foo() {
        class Local {
            String foo() { return null; }
        }
        new Local().foo();
        return "";
    }
}