@echo off

rem echo run a module (%1)

rem echo the following environmental variables must be set: JAVA_HOME, VERTX_HOME, MODS_HOME, HYDRO_HOME

set LIB_DIR=%VERTX_HOME%\lib

set CLUSTER_PARAMS=-instances %3 -cluster -cluster-host 127.0.0.1

set VERTX_OPTS="-Dvertx.mods=%MODS_HOME% -Djava.util.logging.config.file=%VERTX_HOME%\conf\logging.properties"

set MOD_RUN_LABEL=%1

set MOD_RUN_PORT=%2

set MOD_RUN_DIR=%MODS_HOME%\%MOD_RUN_LABEL%

echo %VERTX_HOME%\bin\vertx runmod %MOD_RUN_LABEL% -conf %HYDRO_HOME%\conf\mods\%MOD_RUN_LABEL%.conf %CLUSTER_PARAMS% -cluster-port %MOD_RUN_PORT%

%VERTX_HOME%\bin\vertx runmod %MOD_RUN_LABEL% -conf %HYDRO_HOME%\conf\mods\%MOD_RUN_LABEL%.conf %CLUSTER_PARAMS% -cluster-port %MOD_RUN_PORT%
