public class TestContinueInSwitchInForeach {
    private int a = 0;
    private int b = 0;
    private int c = 0;

    public void foo(char[] cc) {
        for (char ccc : cc) {
            if (ccc == ';') {
                continue;
            }
            switch (ccc) {
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
        Loop: for (char ccc : cc) {
            if (ccc == ';') {
                continue;
            }
            switch (ccc) {
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

    public void bar(char[] cc) {
        for (int i = 0; i < 10; i++) {
            switch (cc[i]) {
                case ';':
                    continue;
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

    public void barWithLabel(char[] cc) {
        Loop: for (int i = 0; i < 10; i++) {
            switch (cc[i]) {
                case ';':
                    continue Loop;
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
}
