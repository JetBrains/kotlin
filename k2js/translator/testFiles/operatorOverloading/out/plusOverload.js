var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a){
    this.$value = a;
  }
  , get_value:function(){
    return this.$value;
  }
  , plus:function(other){
    {
      return new foo.myInt(this.get_value() + other.get_value());
    }
  }
  });
  return {myInt:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.myInt(3)).plus(new foo.myInt(5)).get_value() == 8;
  }
}
}, {myInt:classes.myInt});
foo.initialize();
