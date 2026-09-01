var result = "";

function box() {
    var $tmp = result;
    result += "fail";
    result = $tmp + "OK";
    return result;
}
