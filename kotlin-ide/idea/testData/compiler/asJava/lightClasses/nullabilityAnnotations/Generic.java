public abstract interface Generic /* Generic*/<N, NN>  {
  @org.jetbrains.annotations.NotNull()
  public abstract NN b(@org.jetbrains.annotations.NotNull() NN);//  b(NN)

  @org.jetbrains.annotations.Nullable()
  public abstract N a1(@org.jetbrains.annotations.Nullable() N);//  a1(N)

  @org.jetbrains.annotations.Nullable()
  public abstract NN b1(@org.jetbrains.annotations.Nullable() NN);//  b1(NN)

  public abstract N a(N);//  a(N)

}