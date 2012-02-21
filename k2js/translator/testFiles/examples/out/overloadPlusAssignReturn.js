var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$contents = new Kotlin.ArrayList;
  }
  , get_contents:function(){
    return this.$contents;
  }
  , add:function(item){
    {
      this.get_contents().add(item);
    }
  }
  , plusAssign:function(rhs){
    {
      this.get_contents().addAll(rhs.get_contents());
    }
  }
  , get:function(index){
    {
      return this.get_contents().get(index);
    }
  }
  });
  return {ArrayWrapper:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var v1 = new Anonymous.ArrayWrapper;
    var v2 = new Anonymous.ArrayWrapper;
    v1.add('foo');
    var v3 = v1;
    v2.add('bar');
    v1.plusAssign(v2);
    if (v1.get_contents().size() == 2 && v3.get_contents().size() == 2)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
}, {ArrayWrapper:classes.ArrayWrapper});
Anonymous.initialize();
