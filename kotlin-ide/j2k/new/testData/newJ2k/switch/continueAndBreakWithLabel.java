//method
void foo() {
Loop:
  while(true) {
    switch(take()) {
      case 1: continue;
      case 2: System.out.println("2"); return;
      case 3: break Loop;
    }
    System.out.println();
  }
}
