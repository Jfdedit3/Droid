# Sauvegarde de la clé de signature DroidDeck

Cette archive est chiffrée avec GPG (AES-256). Elle contient la keystore de production, son mot de passe et les métadonnées de signature. La phrase de déchiffrement est remise séparément au propriétaire et doit être conservée hors du dépôt. Aucun secret en clair ne doit être ajouté ici.

## Récupération

Dans un dossier privé :

```sh
sha256sum -c droiddeck-xclipse920-keystore.tar.gpg.sha256
gpg --output signing-recovery.tar --decrypt droiddeck-xclipse920-keystore.tar.gpg
mkdir -m 700 signing-recovery
tar -xf signing-recovery.tar -C signing-recovery
chmod 600 signing-recovery/*
```

GPG demande la phrase de déchiffrement. Le fichier `signing-password.txt` contient ensuite le mot de passe de la keystore `xclipse920.p12` (alias `xclipse920`). Utiliser ces éléments avec les paramètres de signature du projet. Garder les fichiers déchiffrés et la phrase hors de Git. Cette sauvegarde ne configure pas les secrets GitHub Actions.
