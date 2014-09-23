#!/bin/bash

# run a module ($1)

# the following environmental variables must be set: JAVA_HOME, VERTX_HOME, MODS_HOME, HYDRO_HOME

LIB_DIR=$VERTX_HOME/lib

CLUSTER_PARAMS="-instances $3 -cluster -cluster-host 127.0.0.1"

VERTX_OPTS="-Dvertx.mods=$MODS_HOME -Djava.util.logging.config.file=$VERTX_HOME/conf/logging.properties"

MOD_RUN_LABEL=$1

MOD_RUN_PORT=$2

MOD_RUN_DIR=$MODS_HOME/$MOD_RUN_LABEL

export MOD_RUN_LABEL MOD_RUN_DIR MOD_RUN_PORT LIB_DIR CLUSTER_PARAMS MODS_HOME VERTX_OPTS

echo "exec: " $VERTX_HOME/bin/vertx runmod $MOD_RUN_LABEL -conf $HYDRO_HOME/conf/mods/$MOD_RUN_LABEL.conf $CLUSTER_PARAMS -cluster-port $MOD_RUN_PORT

$VERTX_HOME/bin/vertx runmod $MOD_RUN_LABEL -conf $HYDRO_HOME/conf/mods/$MOD_RUN_LABEL.conf $CLUSTER_PARAMS -cluster-port $MOD_RUN_PORT
