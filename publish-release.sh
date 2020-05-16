#!/bin/sh
docker login -u $DOCKER_USER -p $DOCKER_PASS
docker push esmartit/access-point-incoming-endpoint:"$1"
docker push esmartit/access-point-incoming-endpoint:latest
helm package access-point-incoming-endpoint --version "$1" --app-version "$1"
touch version.txt
echo "$1" >> version.txt
exit