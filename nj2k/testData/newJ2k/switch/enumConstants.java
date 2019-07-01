enum ColorEnum {
    GREEN
}

class MyClass {
    int method(ColorEnum colorEnum) {
        switch (colorEnum) {
            case GREEN: return 1;
            default: return 2;
        }
    }
}