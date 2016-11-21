var result = "";

function box() {
    var $tmp;
    $tmp = result;
    result += "fail";
    result = $tmp + "OK";
    return result;
}