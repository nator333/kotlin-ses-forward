#!/usr/bin/env bash

./gradlew :app:spotlessApply
./gradlew :app:build
serverless deploy