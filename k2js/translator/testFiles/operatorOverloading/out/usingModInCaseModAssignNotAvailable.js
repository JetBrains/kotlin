var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$p = 'yeah';
  }
  , get_p:function(){
    return this.$p;
  }
  , set_p:function(tmp$0){
    this.$p = tmp$0;
  }
  , mod:function(other){
    {
      return new foo.A;
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new foo.A;
    var d = c;
    c = c.mod(new foo.A);
    return c != d && c.get_p() == 'yeah';
  }
}
}, {A:classes.A});
foo.initialize();
