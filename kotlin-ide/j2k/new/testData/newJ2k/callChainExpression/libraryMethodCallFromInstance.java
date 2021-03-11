class Library {
  void call() {}

  String getString() { return ""; }
}

class User {
  void main() {
    Library lib = new Library();
    lib.call();
    lib.getString().isEmpty();

    new Library().call();
    new Library().getString().isEmpty();
  }
}