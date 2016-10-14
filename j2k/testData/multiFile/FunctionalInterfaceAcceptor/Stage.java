package test;
public class Stage {
    public void context(Acceptor acceptor) {
        acceptor.acceptFace(new Face() {
            @Override public void subject(String p) {
                System.out.println(p);
            }
        });
        acceptor.setFace(new Face() {
            @Override public void subject(String p) {
                System.out.println(p);
            }
        });
    }
}