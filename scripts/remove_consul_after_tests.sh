#!/bin/sh

ps -ef | grep consul | awk '{print $2}' | xargs kill -9

