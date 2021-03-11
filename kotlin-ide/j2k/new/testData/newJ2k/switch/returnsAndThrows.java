//method
int foo(int a) {
  switch(a) {
    case 1: System.out.println("1"); return 1;
    case 2: System.out.println("2"); return 2;
    case 3: System.out.println("3"); throw new RuntimeException();
    default: System.out.println("default"); return 0;
  }
}