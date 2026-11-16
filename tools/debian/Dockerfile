FROM --platform=linux/arm64 debian:trixie-slim@sha256:a72b4433de1cf7d6bae0c4cee74d0547f58601a643b3375f070c51600d63b6b2
RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    xfce4-session xfwm4 xfdesktop4 xfce4-panel xfce4-settings thunar xfce4-terminal exo-utils \
    xwayland x11-utils dbus-x11 xdg-utils fonts-dejavu-core ca-certificates \
    libnss3 libnspr4 libasound2t64 libgbm1 libnotify4 libsecret-1-0 libgtk-3-0t64 libxss1 \
    libxtst6 libxrandr2 libxcomposite1 libxdamage1 libxfixes3 libxkbcommon0 libdrm2 \
    libxshmfence1 libatk-bridge2.0-0t64 \
    && apt-get clean && rm -rf /var/lib/apt/lists/* /var/cache/apt/* /tmp/* /var/log/*
