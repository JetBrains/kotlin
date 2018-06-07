// http://jonisalonen.com/2012/from-utf-16-to-utf-8-in-javascript/
String.prototype.toUTF8Array = function() {
    var utf8 = [];
    for (var i = 0; i < this.length; i++) {
        var charCode = this.charCodeAt(i);
        if (charCode < 0x80) utf8.push(charCode);
        else if (charCode < 0x800) {
            utf8.push(
                0xc0 | (charCode >> 6),
                0x80 | (charCode & 0x3f)
            );
        }
        else if (charCode < 0xd800 || charCode >= 0xe000) {
            utf8.push(
                0xe0 | (charCode >> 12),
                0x80 | ((charCode >> 6) & 0x3f),
                0x80 | (charCode & 0x3f)
            );
        }
        // surrogate pair
        else {
            i++;
            // UTF-16 encodes 0x10000-0x10FFFF by
            // subtracting 0x10000 and splitting the
            // 20 bits of 0x0-0xFFFFF into two halves
            charCode = 0x10000 + (((charCode & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
            utf8.push(
                0xf0 | (charCode >> 18),
                0x80 | ((charCode >> 12) & 0x3f),
                0x80 | ((charCode >> 6) & 0x3f),
                0x80 | (charCode & 0x3f)
            );
        }
    }
    return utf8;
};

String.prototype.toInt8Array = function() {
    return new Int8Array(this.toUTF8Array());
};

String.fromUTF8Array = function(data) {
    var utf16Arr = [];
    for (var i = 0; i < data.length; i++) {
        var value = data[i] & 0xFF;

        if (value < 0x80) {
            utf16Arr.push(value);
        } else if (value > 0xBF && value < 0xE0) {
            utf16Arr.push((value & 0x1F) << 6 | data[i + 1] & 0x3F);
            i += 1;
        } else if (value > 0xDF && value < 0xF0) {
            utf16Arr.push((value & 0x0F) << 12 | (data[i + 1] & 0x3F) << 6 | data[i + 2] & 0x3F);
            i += 2;
        } else {
            // surrogate pair
            var charCode = ((value & 0x07) << 18 | (data[i + 1] & 0x3F) << 12 | (data[i + 2] & 0x3F) << 6 | data[i + 3] & 0x3F) - 0x010000;

            utf16Arr.push(charCode >> 10 | 0xD800, charCode & 0x03FF | 0xDC00);
            i += 3;
        }
    }
    return String.fromCharCode.apply(String, utf16Arr);
};