#!/bin/sh
if [ "$ACIS_DB_DIAGNOSTICS" = "true" ]; then
    java -cp /app/probe DatabaseTlsProbe
fi
exec java -jar /app/service.jar
