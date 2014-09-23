#!/bin/bash

# install a module to the mods directory

# the following environmental variables must be set: JAVA_HOME, VERTX_HOME, MODS_HOME, HYDRO_HOME

LIB_DIR=$VERTX_HOME/lib

VERTX_OPTS="-Dvertx.mods=$MODS_HOME"

MOD_RUN_LABEL=$1

MOD_RUN_DIR=$MODS_HOME/$MOD_RUN_LABEL

export MOD_RUN_LABEL MOD_RUN_DIR APP_HOME JAVA_HOME VERTX_HOME LIB_DIR MODS_HOME VERTX_OPTS

echo "exec: " $VERTX_HOME/bin/vertx install $MOD_RUN_LABEL

$VERTX_HOME/bin/vertx install $MOD_RUN_LABEL