//method
public String foo(int i, int j) {
    switch (i) {
        case 0:
            switch (j) {
                case 1:
                    return "0, 1";
                case 2:
                    return "0, 2";
            }
        case 1:
            return "1, x";
        default:
            return "x, x";
}
