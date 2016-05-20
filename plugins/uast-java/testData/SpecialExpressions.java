class SpecialExpressions {
    boolean test() {
        assert 5 > 3;
        assert 5 > 3 : "Message";

        synchronized (this) {
            System.out.println("A");
        }

        int a = 5, b = 7, c;

        while (a > 0) {
            if (a == 3) {
                break;
            }
            if (a % 5 == 0) {
                continue;
            }
            a--;
        }

        this.test();
        super.hashCode();

        String x;
        switch (a) {
            case 1: {
                x = "1";
                break;
            }
            case 3: x = "3";
            case 4: x = "4";
            default: x = "";
        }

        if (System.getProperty("abc", "").equals("1")) {
            throw new AssertionError("Err");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        } finally {
            a = 3;
        }

        {
            a = 5;
        }

        return true;
    }
}