var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 0;
  }
  , get_a:function(){
    return this.$a;
  }
  , set_a:function(tmp$0){
    this.$a = tmp$0;
  }
  });
  return {Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, get_b:function(receiver){
  {
    return receiver.get_a() * 3;
  }
}
, set_b:function(receiver, c){
  {
    receiver.set_a(c - 1);
  }
}
, box:function(){
  {
    var c = new foo.Test;
    if (c.get_a() != 0)
      return false;
    if (foo.get_b(c) != 0)
      return false;
    c.set_a(3);
    if (foo.get_b(c) != 9)
      return false;
    foo.set_b(c, 10);
    if (c.get_a() != 9)
      return false;
    if (foo.get_b(c) != 27)
      return false;
    return true;
  }
}
}, {Test:classes.Test});
foo.initialize();
