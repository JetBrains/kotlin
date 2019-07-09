//class
enum Color {
    WHITE(21), BLACK(22), RED(23), YELLOW(24), BLUE(25);

    private int code;

    private Color(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}