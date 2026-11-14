FROM ubuntu:latest
LABEL authors="ilade"

ENTRYPOINT ["top", "-b"]