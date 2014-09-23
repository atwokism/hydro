@echo off

rem run the ant deploy-dist task for compiling, building and deploying modules to the vert.x mods directory

ant deploy-dist -Dmod.name=%1
