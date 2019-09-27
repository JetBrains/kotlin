public class TestContinueInSwitchInFor {
    public void foo(char[] cc) {
        int a = 0;
        int b = 0;
        int c = 0;
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
}
