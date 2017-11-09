public class SwitchDemo {
    public static int test(int i) {
        String monthString = "<empty>";
        switch (i) {
            case 1:  print(1);
            case 2:  print(2);
            case 3:  print(3);
            default:
            case 4:  print(4);
            case 5:  print(5); break;
            case 6:  print(6);
            case 7:  print(7);
            case 8:  print(8);
            case 9:  print(9);
            case 10: print(10);
            case 11: print(11);
            case 12: monthString = "December"; break;
        }
        String status="";
        switch (status) {
            case "init":
            case "dial":
            case "transmit":
                return 0x111111;
            case "ok":
                return 0xFF006600;
            case "cancel":
                return 0xFF666666;
            case "fail":
            case "busy":
            case "error":
            default:
                return 0xFF660000;
        }
    }
}