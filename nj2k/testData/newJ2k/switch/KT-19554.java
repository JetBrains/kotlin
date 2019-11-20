public class TestContinueInSwitchInFor {
    private int a = 0;
    private int b = 0;
    private int c = 0;

    public void foo(char[] cc) {
        for (int i = 0; i < cc.length && cc[i] != ';'; ++i) {
            switch (cc[i]) {
                case ' ':
                    continue;
                case 'a':
                    a++;
                    break;
                case 'b':
                    b++;
                    break;
                case 'c':
                    c++;
                    break;
            }
        }
        System.out.println("a = " + a + "; b = " + b + "; c = " + c);
    }

    public void fooWithLabel(char[] cc) {
        Loop: for (int i = 0; i < cc.length && cc[i] != ';'; ++i) {
            switch (cc[i]) {
                case ' ':
                    continue Loop;
                case 'a':
                    a++;
                    break;
                case 'b':
                    b++;
                    break;
                case 'c':
                    c++;
                    break;
            }
        }
        System.out.println("a = " + a + "; b = " + b + "; c = " + c);
    }

    public void fooWithNestedLabel(char[] cc) {
        Loop: for (int i = 0; i < cc.length && cc[i] != ';'; ++i) {
            switch (cc[i]) {
                case ' ':
                    continue;
                case 'a':
                    a++;
                    break;
                case 'b':
                    b++;
                    break;
                case 'c':
                    c++;
                    break;
                case 'x':
                    for (int i = 0; i < cc.length && cc[i] != ';'; ++i) {
                        switch (cc[i]) {
                            case ' ':
                                continue Loop;
                            case 'a':
                                a++;
                                break;
                            case 'b':
                                b++;
                                break;
                            case 'c':
                                c++;
                                break;
                        }
                        break;
                    }
            }
        }
        System.out.println("a = " + a + "; b = " + b + "; c = " + c);
    }
}
