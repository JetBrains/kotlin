fun main(args:Array<String>) {
    val old = pref.getString("", "")
    pref.edit().putString("", "").apply()
    <selection>LocalBroadcastManager.getInstance(this)</selection>.sendBroadcast(Intent(""))
}