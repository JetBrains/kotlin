public abstract class A /* A*/<T extends A<T>>  extends B<java.util.Collection<? extends T>> implements C<T> {
  public  A();//  .ctor()



public class Inner /* A.Inner*/<D>  extends B<java.util.Collection<? extends T>> implements C<D> {
  public  Inner();//  .ctor()

}public final class Inner2 /* A.Inner2*/<X>  extends A<T>.Inner<X> implements C<X> {
  public  Inner2();//  .ctor()

}}