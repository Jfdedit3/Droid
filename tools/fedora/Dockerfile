FROM --platform=linux/arm64 fedora:43@sha256:f01112b9a1f1d0504ad962d9adfa401425033f8ab067e0a821904ebdde5b8ca5
COPY packages.txt /tmp/droiddeck-packages.txt
RUN dnf -y --setopt=install_weak_deps=False --setopt=tsflags=nodocs install $(cat /tmp/droiddeck-packages.txt) \
    && dnf clean all && rm -rf /var/cache/* /tmp/* /var/log/*
RUN ln -sfn libbz2.so.1 /usr/lib64/libbz2.so.1.0
