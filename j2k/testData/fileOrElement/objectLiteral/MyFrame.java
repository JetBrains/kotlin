//file
package demo;

interface WindowListener {
  void windowClosing ();
}

interface EmptyWindowListener {
}

open class EmptyWindowAdapter : EmptyWindowListener {}

class WindowAdapter implements WindowListener {
  public void windowClosing () {
  }
}

class Frame {
  public void addWindowListener(WindowListener listener){}
}

final class Client extends Frame {
  Client() {
    WindowAdapter a = new WindowAdapter() {
      @Override
      public void windowClosing () {
      }
    };

    addWindowListener(a);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing () {
      }
    });

    EmptyWindowListener b = new EmptyWindowListener() {};
    EmptyWindowAdapter c = new EmptyWindowAdapter() {};
  }
}