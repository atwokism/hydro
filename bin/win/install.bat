@echo off

rem install a module to the mods directory

rem echo the following environmental variables must be set: JAVA_HOME, VERTX_HOME, MODS_HOME, HYDRO_HOME

set LIB_DIR=%VERTX_HOME%\lib

set VERTX_OPTS="-Dvertx.mods=%MODS_HOME% -Dhttp.proxyHost=%2 -Dhttp.proxyPort=%3 -Dhttp.authUser=%4 -Dhttp.authPass=%5"

set MOD_RUN_LABEL=%1

set MOD_RUN_DIR=%MODS_HOME%\%MOD_RUN_LABEL%

echo "exec: " %VERTX_HOME%\bin\vertx install %MOD_RUN_LABEL%
echo "VERTX_OPTS: " %VERTX_OPTS%

%VERTX_HOME%\bin\vertx install %MOD_RUN_LABEL%


