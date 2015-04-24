#!/bin/bash

mkdir -p ~/tmp/ca
cd ~/tmp/ca

curl http://www.startssl.com/certs/ca.crt -O
curl http://www.startssl.com/certs/sub.class1.server.ca.crt -O
curl http://www.startssl.com/certs/sub.class2.server.ca.crt -O
curl http://www.startssl.com/certs/sub.class3.server.ca.crt -O
curl http://www.startssl.com/certs/sub.class4.server.ca.crt -O

for crt in *.crt; do
  keytool -import -trustcacerts -keystore ${JAVA_HOME}/jre/lib/security/cacerts -storepass changeit -noprompt -alias ${crt} -file ${crt};
done

