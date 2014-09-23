@echo off

rem run the ant 'destroy-dist' task for deleting the module build project and removing it from the vert.x mods directory

ant destroy-dist -Dmod.name=%1
