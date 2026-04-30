#!/bin/bash
cd /AstrBot/data/workspaces/Notion_Tally_Book
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew assembleDebug 2>&1 | tail -100
