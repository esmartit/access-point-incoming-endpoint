#!/bin/sh
docker build -t esmartit/access-point-incoming-endpoint:"$1" -t esmartit/access-point-incoming-endpoint:latest .
exit