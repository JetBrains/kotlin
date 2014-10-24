//file
package demo;

interface WindowListener {
  void windowClosing ();
}

class WindowAdapter implements WindowListener {
  public void windowClosing () {
  }
}

class Frame {
  public void addWindowListener(WindowListener listener){}
}

public final class Client extends Frame {
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
  }
}