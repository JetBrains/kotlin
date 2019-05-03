class Library {
  static void call() {}

  static String getString() { return ""; }
}

class User {
  void main() {
    Library.call();
    Library.getString().isEmpty();
  }
}