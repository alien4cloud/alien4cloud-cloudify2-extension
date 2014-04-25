@echo off

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome

@rem @echo JAVA_HOME environment variable is set to %JAVA_HOME% in "<GigaSpaces Root>\bin\setenv.bat"
set JAVACMD=%JAVA_HOME%\bin\java
goto endOfJavaHome

:noJavaHome
@rem @echo The JAVA_HOME environment variable is not set. Using the java that is set in system path.
set JAVACMD=java

:endOfJavaHome

if "%LUS_IP_ADDRESS%" == "" set LUS_IP_ADDRESS=localhost:4174
echo Deploying custom events on locator(s) %LUS_IP_ADDRESS%

set HOME=%~dp0..
set CLASSPATH="%HOME%\lib\*"
set ARGS=-name events -locators %LUS_IP_ADDRESS% -pu %HOME%\deploy\cloudify-events-rest.war

%JAVACMD% -cp %CLASSPATH% fr.fastconnect.cloudify.GigaSpacesPUDeployer %ARGS%