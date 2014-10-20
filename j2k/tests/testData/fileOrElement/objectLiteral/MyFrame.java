//file
package demo;

class WindowAdapter {
  public void windowClosing () {
  }
}

public final class Client extends Frame {
  Client() {
    WindowAdapter a = new WindowAdapter() {
      @Override
      public void windowClosing () {
      }
    }

    addWindowListener(a);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing () {
      }
    });
  }
}