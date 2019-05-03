//method
public String foo(int i, int j) {
    switch (i) {
        case 0:
            if (j > 0) {
                return "1"
            }
        case 1:
            return "2";
        default:
            return "3";
    }
}