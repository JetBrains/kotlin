//method
public String foo(int i, int j) {
    switch (i) {
        case 0:
            if (j > 0) {
                return "1"
            } else{
                return "2"
            }
        case 1:
            return "3";
        default:
            return "4";
    }
}