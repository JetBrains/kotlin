foo = Namespace.create({initialize:function(){
}
, box:function(){
  return !false;
}
});

function test() {
    return foo.box()
}
