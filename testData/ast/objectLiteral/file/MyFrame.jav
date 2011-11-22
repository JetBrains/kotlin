package demo;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public final class Client extends Frame {
  Client() {
    WindowAdapter a = new WindowAdapter() {
      @Override
      public void windowClosing (WindowEvent e) {
        dispose();
      }
    }

    addWindowListener(a);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing (WindowEvent e) {
        dispose();
      }
    });
  }
}