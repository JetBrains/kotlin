class Library {
  final static java.io.PrintStream ourOut;
}

class User {
  void main() {
    Library.ourOut.print();
  }
}