interface Inter {
    String something();
}

class Test extends Abstract implements Inter {
    String something() {
        return "123";
    }
}