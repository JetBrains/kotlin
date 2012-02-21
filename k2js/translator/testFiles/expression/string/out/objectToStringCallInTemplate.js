var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(i){
    this.$i = i;
  }
  , get_i:function(){
    return this.$i;
  }
  , set_i:function(tmp$0){
    this.$i = tmp$0;
  }
  , toString:function(){
    {
      return 'a' + this.get_i();
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
    var p = new foo.A(2);
    var n = new foo.A(1);
    if (p + n != 'a2a1') {
      return false;
    }
    if (new foo.A(10) != 'a10') {
      return false;
    }
    return true;
  }
}
}, {A:classes.A});
foo.initialize();
