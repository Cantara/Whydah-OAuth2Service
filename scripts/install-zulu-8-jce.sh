#!/bin/bash
wget http://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip
unzip ZuluJCEPolicies.zip
cd ZuluJCEPolicies
cp *.jar /usr/lib/jvm/zulu-8/jre/lib/security/
