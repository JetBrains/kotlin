class ControlStructures {
  public static void main(String[] args) {
    if (args.length == 0) {
      return;
    }

    String mode = args.length == 1 ? "singleArg" : "multiArgs";

    for (String arg : args) {
      System.out.println(arg);
    }

    for (int i = 0; i < args.length; ++i) {
      System.out.println(i + ": " + args[i]);
    }

    int i = 0;
    while (i < args.length) {
      System.out.println("Index " + i);
      i++;
    }

    i = 0;
    do {
      System.out.println(i);
      i += 1;
    } while (i < args.length);
  }
}
