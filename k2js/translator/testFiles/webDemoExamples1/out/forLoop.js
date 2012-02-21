var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    var tmp$1;
    var tmp$0;
    {
      tmp$0 = Kotlin.arrayIterator(args);
      while (tmp$0.hasNext()) {
        var arg = tmp$0.next();
        {
          Kotlin.println(arg);
        }
      }
    }
    Kotlin.println();
    {
      tmp$1 = Kotlin.arrayIndices(args).iterator();
      while (tmp$1.hasNext()) {
        var i = tmp$1.next();
        {
          Kotlin.println(args[i]);
        }
      }
    }
  }
}
}, {});
Anonymous.initialize();
