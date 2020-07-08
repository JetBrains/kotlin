public final class Prop /* Prop*/ {
  private final java.lang.Object someProp;

  @null()
  public  Prop();//  .ctor()

}

final class null /* null*/ {
  private  ();//  .ctor()

}

final class C /* C*/ {
  private final int y;

  private final kotlin.jvm.functions.Function0<java.lang.Object> initChild;

  @null()
  public  C(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public final kotlin.jvm.functions.Function0<java.lang.Object> getInitChild();//  getInitChild()

  public final int getY();//  getY()

}

final class null /* null*/ {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  ();//  .ctor()

}

public final class ValidPublicSupertype /* ValidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  ValidPublicSupertype();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();//  getX()

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

public abstract interface I /* I*/ {
}

public final class InvalidPublicSupertype /* InvalidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  InvalidPublicSupertype();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();//  getX()

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}