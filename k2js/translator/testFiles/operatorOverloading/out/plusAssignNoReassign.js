var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$message = '';
  }
  , get_message:function(){
    return this.$message;
  }
  , set_message:function(tmp$0){
    this.$message = tmp$0;
  }
  , plusAssign:function(other){
    {
      this.set_message(this.get_message() + '!');
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
    c.plusAssign(new foo.A);
    c.plusAssign(new foo.A);
    return c.get_message() == '!!';
  }
}
}, {A:classes.A});
foo.initialize();
