package client

import server.Data

class Client<T: Data, X> where X: Data {

}

val c = Client<Data/*, [ERROR]*/>()