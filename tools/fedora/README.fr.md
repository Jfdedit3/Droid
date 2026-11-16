# Fedora 43 ARM64 et Xfce

Fedora remplace le choix Debian dans le lanceur. Ubuntu/Plasma reste disponible. L'ancien système Debian n'est pas effacé à la mise à jour : le bouton « Supprimer l'ancien Debian » permet de le retirer après confirmation, y compris ses fichiers personnels.

Fedora possède sa propre racine `files/fedora-xfce` et son propre dossier personnel. Le client Steam est exécuté dans cette racine, sur le même affichage X11 que Xfce. Il ne demande pas au lanceur Android de fermer Fedora. Les fichiers Steam, les jeux, Proton, FEX, les pilotes et Brave déjà installés sont partagés par des montages PRoot : aucune copie complète d'Ubuntu n'est exécutée pour ouvrir Steam. Les préférences graphiques et Proton existantes sont conservées.

Le pont bubblewrap utilise la racine et les montages de la session active. Pour les outils ARM de FEX dans un environnement x86, les bibliothèques Fedora `/usr/lib64` sont placées dans un espace ARM séparé. Le premier `env` démarre avec le chargeur ARM et ce chemin explicite ; les bibliothèques x86 de `/usr/lib64` ne sont pas masquées.

`packages.txt` et le Dockerfile décrivent une reconstruction ARM64. Les dépôts Fedora évoluent ; `packages.lock.txt` et `manifest.json` décrivent l'archive distribuée. Pour reprendre exactement cet asset, extraire `assets/bundled/fedora-xfce-arm64.tgz` de l'APK et vérifier son SHA-256.

Dans l'environnement de préparation x86, DNF a installé les paquets ARM64 dans une racine séparée, avec vérification GPG et TLS, sans scripts de démarrage de services. Les caches GSettings, GdkPixbuf, MIME, polices et certificats ont ensuite été générés avec les programmes ARM64 sous PRoot/QEMU. Le système ne démarre ni systemd ni un noyau Fedora : il utilise le noyau Android et une session Xfce autonome.

Avant l'archivage, supprimer les données de test, journaux, sockets et certificats propres à l'environnement de test ; remettre les comptes root à UID/GID 0. Le programme Android adapte ensuite ces comptes à son UID réel. Ne pas ajouter `--link2symlink` ou forcer `PROOT_NO_SECCOMP` pour le bureau. Le correctif de verrou X11 est chargé seulement dans Xwayland.

L'archive utilise GNU tar, avec `--hard-dereference`, liens symboliques conservés, propriétaire 0 et format POSIX. Elle conserve les mentions légales des paquets sous `/usr/share/licenses`. Les sources et licences se trouvent dans les dépôts source officiels Fedora et https://src.fedoraproject.org/ .

Les tests d'affichage, de chargement PNG sans root, du bureau Xfce et de l'espace de bibliothèques ARM sont décrits dans `validation.json`. La validation sous émulation ne mesure pas les performances ni la compatibilité des jeux sur le Xclipse 920.

Le bureau est proposé en aperçu. Le test du vrai client Steam charge ses bibliothèques ARM64, mais s'arrête dans l'émulation avec « pthreads futex robust_list is corrupt ». Les appels `get_robust_list` et `process_vm_readv` sur soi-même retournent ENOSYS sous QEMU dans cet environnement. Le démarrage complet et les jeux doivent donc encore être vérifiés sur le S22+ ; Ubuntu reste disponible. Les erreurs Steam sont conservées dans les journaux de session, même si le bureau est ensuite fermé normalement.

Les bibliothèques ARM Fedora ne prennent pas la place des bibliothèques x86 dans les environnements FEX. L'alias `/usr/lib64/libbz2.so.1.0` pointe vers la même bibliothèque bzip2 ABI 1 fournie par Fedora ; il ne remplace aucune autre version d'ABI.


Le complément de jeu de la version .12 est décrit par gaming-manifest.json et gaming-packages.lock.txt. Il est distinct de l’image de base pour mettre à jour les installations existantes. L’APK contient son archive exacte dans assets/bundled/fedora-gaming-arm64.tgz. Les RPM Fedora ont été installés avec vérification TLS/GPG, --forcearch=aarch64 et scripts désactivés, à partir de l’état RPM de l’image de base. Seuls usr/lib64, les licences et les données libinput/libwacom sont exportés. Le certificat du proxy de compilation n’est pas exporté. La bibliothèque libstdc++6 vient du paquet Ubuntu vérifié dont le SHA-256 est enregistré dans le manifeste ; son copyright et l’exception d’exécution GCC sont inclus. Aucun changement de glibc n’est effectué.
