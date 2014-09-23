@echo off

rem run the ant create-dist task for creating a mods build project directory

ant create-dist -Dmod.name=%1
