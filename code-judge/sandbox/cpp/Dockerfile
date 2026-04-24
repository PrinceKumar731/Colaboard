# Sandbox image for C++ submissions (GCC 12, C++17).
FROM debian:bullseye-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ coreutils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -M -s /usr/sbin/nologin sandbox

WORKDIR /code

USER sandbox
