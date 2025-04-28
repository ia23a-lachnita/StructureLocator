@echo off
call gradlew --stop
call gradlew clean
call gradlew --refresh-dependencies
call gradlew idea --no-daemon
call gradlew genIntellijRuns --no-daemon