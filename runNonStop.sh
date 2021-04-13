#!/bin/sh
echo Start ..

PATH_PROG=.
PATH_LOG=$PATH_PROG/log

LOG_CONF=$PATH_PROG/properties/logging.properties

VMparam="-server -Xms256m -Xmx256m"
VMparam="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -Xloggc:$PATH_LOG/gc-prog-01.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=11 -XX:GCLogFileSize=12M $VMparam"
#VMparam="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6101 $VMparam"
VMparam="-Djava.util.logging.config.file=$LOG_CONF $VMparam"
VMparam="-Dapp.path.config=./properties $VMparam"

echo Start .. > runNonStop.log
date >> runNonStop.log

while true
do
  nice -15 java $VMparam -jar $PATH_PROG/mailConsumer-1.0.0.jar -v
  ## Check result .. ##
  if [ $? -eq 0 ]
  then
    echo "Run = Success: continue to process .."
  else
    echo "Failure: .." >&2
    exit $?
  fi
  sleep 11
done

echo --
echo End of script!

exit 0
