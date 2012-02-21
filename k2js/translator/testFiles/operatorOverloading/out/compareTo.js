var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(t){
    this.$i = t;
  }
  , get_i:function(){
    return this.$i;
  }
  , set_i:function(tmp$0){
    this.$i = tmp$0;
  }
  , compareTo:function(other){
    {
      return this.get_i() - other.get_i();
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
    return (new foo.A(3)).compareTo(new foo.A(2)) > 0 && (new foo.A(2)).compareTo(new foo.A(2)) >= 0 && (new foo.A(1)).compareTo(new foo.A(0)) >= 0 && (new foo.A(2)).compareTo(new foo.A(2)) <= 0 && (new foo.A(3)).compareTo(new foo.A(4)) <= 0 && (new foo.A(0)).compareTo(new foo.A(100)) < 0;
  }
}
}, {A:classes.A});
foo.initialize();
