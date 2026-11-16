# Debian 13 ARM64 + Xfce 4.20

Ce système est indépendant du runtime Ubuntu 26.04/Plasma. Steam, RADV et Proton continuent à utiliser Ubuntu. Debian est destiné au bureau, aux fichiers et à Brave ; il n'ajoute pas une seconde pile de jeux.

`packages.lock.txt` recense les versions du système livré. `manifest.json` contient l'empreinte de l'archive intégrée dans `assets/bundled/debian-xfce-arm64.tgz` de l'APK. Pour reproduire exactement l'empaquetage Android, extraire cet asset de l'APK fourni, vérifier le SHA-256 et le placer dans `app/src/main/assets/bundled/`.

Pour reconstruire une base Debian mise à jour, le Dockerfile fournit les paquets et l'image officielle utilisés. Sur une machine ARM64, ou avec QEMU/binfmt configuré, construire le Dockerfile puis exporter le système de fichiers du conteneur. Les dépôts Debian évoluent : une reconstruction ultérieure peut avoir de nouvelles versions ; vérifier le manifeste et changer l'empreinte dans DebianDesktop.kt. Dans cet environnement, les installations ont été réalisées sous PRoot récent et QEMU 10.0.13 avec ces mêmes paquets ARM64, puis testées avec Weston 14 en mode headless/Pixman.

Avant l'archivage : supprimer les caches, fichiers de test, sockets et identifiants de machine ; conserver les licences dans /usr/share/doc ; garder root:x:0:0 dans /etc/passwd et root:x:0 dans /etc/group. Le programme Android adapte ces deux entrées à son UID réel lors de l'installation : D-Bus utilise les identifiants Unix réels du processus Android. Debian fonctionne sans faux UID 0 et sans service système privilégié.

Créer l'archive avec GNU tar, liens physiques déréférencés mais liens symboliques conservés :

```
tar --sort=name --mtime='UTC 2026-11-16' --owner=0 --group=0 --numeric-owner --hard-dereference --format=posix --pax-option=delete=atime,delete=ctime -czf debian-xfce-arm64.tar.gz -C ROOTFS .
```

L'APK installe cette archive localement, avec vérification SHA-256 et activation par renommage atomique. Prévoir 2 Go libres. Les fichiers personnels sont sous files/debian-xfce/root ; ceux d'Ubuntu restent sous files/linuxfs/root. Seul le programme Brave de l'installation Ubuntu est lié à Debian ; son profil utilisateur est distinct.

Les sources des paquets et leurs licences restent celles de Debian : https://deb.debian.org/debian/ et https://security.debian.org/debian-security/ ; les sources peuvent être retrouvées par version sur https://snapshot.debian.org/ . Les mentions de droits sont conservées dans les répertoires /usr/share/doc/*/copyright du système livré.

Les tests vérifient l'extraction, les chemins d'archive et le cycle de vie du lanceur. Le lancement réel de Xwayland, xfwm4, xfce4-panel, Thunar et xfdesktop a été contrôlé sous émulation ARM64. L'affichage, le son, le clavier et Brave sur le S22+ restent à vérifier sur l'appareil.
