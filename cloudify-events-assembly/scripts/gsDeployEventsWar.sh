#!/bin/sh

if [ -z "${JAVA_HOME}" ]; then
    # echo "The JAVA_HOME environment variable is not set. Using the java that is set in system path."
  JAVACMD=java
else
    # echo JAVA_HOME environment variable is set to ${JAVA_HOME} in "<GigaSpaces Root>\bin\setenv.sh"
  JAVACMD="${JAVA_HOME}/bin/java"
fi
export JAVACMD

if [ -z "${LUS_IP_ADDRESS}" ]; then
    # echo "The LUS_IP_ADDRESS environment variable is not set. Using localhost:4174."
  LUS_IP_ADDRESS="localhost:4174"
fi
echo Deploy on locator(s) $LUS_IP_ADDRESS

cd `dirname $0`
HOME=$PWD/..
cd $OLDPWD

CLASSPATH="$HOME"/lib/*
ARGS="-name events -locators $LUS_IP_ADDRESS -pu $HOME/deploy/cloudify-events-rest.war"

$JAVACMD -cp "$CLASSPATH" fr.fastconnect.cloudify.GigaSpacesPUDeployer $ARGS