/**
 * Created by user on 7/7/16.
 */

class InvalidProtocolBufferException(
        override val message: String?    = null,
        override val cause  : Throwable? = null
    ) : Throwable(message, cause) {

}