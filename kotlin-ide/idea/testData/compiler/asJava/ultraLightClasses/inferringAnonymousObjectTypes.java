public final class Prop /* Prop*/ {
  private final java.lang.Object someProp;

  @null()
  public  Prop();

}

final class null /* null*/ {
  private  ();

}

final class C /* C*/ {
  private final int y;

  private final kotlin.jvm.functions.Function0<java.lang.Object> initChild;

  @null()
  public  C(int);

  @org.jetbrains.annotations.NotNull()
  public final kotlin.jvm.functions.Function0<java.lang.Object> getInitChild();

  public final int getY();

}

final class null /* null*/ {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();

  private  ();

}

public final class ValidPublicSupertype /* ValidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  ValidPublicSupertype();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();

  public void run();

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();

  public void run();

}

public abstract interface I /* I*/ {
}

public final class InvalidPublicSupertype /* InvalidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  InvalidPublicSupertype();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();

  public void run();

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();

  public void run();

}