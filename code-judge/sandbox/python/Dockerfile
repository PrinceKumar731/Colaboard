# Sandbox image for Python 3 submissions.
FROM python:3.11-alpine

RUN addgroup -S sandbox && adduser -S sandbox -G sandbox \
    && pip install --no-cache-dir --quiet --upgrade pip \
    && apk del wget curl 2>/dev/null || true

WORKDIR /code

USER sandbox
