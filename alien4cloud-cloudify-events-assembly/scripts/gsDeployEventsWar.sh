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
echo "Deploying custom events on locator(s) $LUS_IP_ADDRESS"

cd `dirname $0`
HOME=$PWD/..
cd $OLDPWD

CLASSPATH="$HOME"/lib/*
ARGS="-name events -locators $LUS_IP_ADDRESS -pu $HOME/deploy/alien4cloud-cloudify-events.war"

# look for the username and password to connect to the manager if ssl is activated
PROPS_FILE=

if [ -f "$HOME/events.properties" ]; then
  PROPS_FILE="$HOME/events.properties"
elif [ -f "$HOME/../events.properties" ]; then
  PROPS_FILE="$HOME/../events.properties"
fi

if [ ! -z "$PROPS_FILE" ]; then
  CLOUD_USERNAME=`grep cloudUsername $PROPS_FILE | cut -d "=" -f2-`
  CLOUD_PASSWORD=`grep cloudPassword $PROPS_FILE | cut -d "=" -f2-`
  if [ ! -z "$CLOUD_USERNAME" ]; then
    ARGS="$ARGS -username $CLOUD_USERNAME"
  fi
  if [ ! -z "$CLOUD_PASSWORD" ]; then
    ARGS="$ARGS -password $CLOUD_PASSWORD"
  fi
fi

$JAVACMD -cp "$CLASSPATH" alien4cloud.paas.cloudify2.events.GigaSpacesPUDeployer $ARGS
