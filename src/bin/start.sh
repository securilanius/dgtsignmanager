#!/bin/bash

# root of the package
PACKAGE_HOME=$(cd "$(dirname "$0")";cd ..;pwd)

#>>>>>>>>>>>>>>>>>>>>>>>>>> modification begin
# bootstrap class
MAIN_CLASS=com.irkut.tc.io.IoApplication

# check whether the program has been executed
PIDs=`jps -l | grep $MAIN_CLASS | awk '{print $1}'`
if [ -n "${PIDs}" ]; then
    echo "Ошибка запуска. Приложение уже запущено. PID:${PIDs}"
    exit 1
fi

# classpath
CLASSPATH="${PACKAGE_HOME}/classes:${PACKAGE_HOME}/lib/*"

#JVM startup parameters
JAVA_OPTS="-server -Xmx2g -Xms2g -Xmn256m -XX:PermSize=128m"