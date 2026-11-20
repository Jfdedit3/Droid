# Droid

Variante ARM64 de [DroidDeck 0.7.1](https://github.com/Droid-Deck/DroidDeck), adaptée au Samsung S22+ / Xclipse 920. Version des sources : **0.7.1-xclipse920.15-preview**.

Le dépôt contient les sources complètes de l’application, les tests, les outils et les modifications. Les crédits et licences amont sont conservés ; la présentation originale est disponible dans [README.upstream.md](README.upstream.md).

Cette version intègre les **290 commits ajoutés à la branche officielle main depuis la 0.7.1**, au commit [56986b5](https://github.com/Droid-Deck/DroidDeck/commit/56986b5999588463753b6db47b4de2d357d21e30). C’est un **aperçu**, pas une nouvelle version stable officielle. La provenance des composants est dans [release/upstream-sync.json](release/upstream-sync.json).

Les ajouts Xclipse sont conservés : profil appliqué une fois, Brave, FG léger adaptatif et réglages par jeu, journaux d’échec, téléchargements et deux bureaux séparés. Ubuntu/Plasma garde son apparence Windows avec sauvegarde ; Fedora/Xfce garde son apparence et reçoit les adaptations nécessaires aux nouvelles API officielles. Les sélections RADV et GE-Proton existantes sont préservées.

## État de validation

Les résultats de compilation, tests et vérifications de signature figurent dans [release/xclipse-build-info.json](release/xclipse-build-info.json). Les tests automatisés ne remplacent pas une validation sur S22+ : le démarrage complet, le rendu et les jeux sur le téléphone restent à vérifier pour cette version. Aucun gain de FPS n’est annoncé.

## Compiler

Voir [BUILD-XCLIPSE.fr.md](BUILD-XCLIPSE.fr.md) pour la procédure complète : JDK 17, Gradle 8.10.2, Android SDK 34 et build-tools 35.0.0. Le build recompile Java/Kotlin et récupère les bibliothèques natives inchangées depuis l’aperçu officiel `main-fccd136`. Les sources des composants natifs et leurs recettes restent disponibles dans le dépôt.

Préparer les entrées volumineuses du build :

- `fedora-xfce-arm64.tgz` : versionné en sept parties dans `build-inputs/fedora-base/`. Exécuter `python3 tools/fedora/assemble-base.py` pour reconstituer le fichier et vérifier son empreinte.
- `brave-arm64.deb` : paquet officiel Brave 1.99.57, URL et empreinte dans les instructions de compilation.

Le complément Fedora de jeu est inclus dans `app/src/main/assets/bundled/`. La sauvegarde chiffrée de la clé est dans `signing/`, sa phrase reste hors du dépôt. Pour une autre distribution, configurer sa propre clé via `DROIDDECK_KEYSTORE` et `DROIDDECK_SIGNING_PASSWORD`.

## Télécharger et consulter les changements

[Page de téléchargement de l’APK](https://droiddeck-xclipse-download.z4trox.chatgpt.site) — accès selon les droits du site. [Notes techniques et historique](XCLIPSE-PERFORMANCE.fr.md).

Les workflows GitHub d’origine sont conservés dans `.github/upstream-workflows/`. Ils ne sont pas activés automatiquement dans ce dépôt et doivent être adaptés avant utilisation.

Pour reconstruire les composants natifs amont, initialiser les sous-modules épinglés avec `git submodule update --init --recursive`, puis suivre les recettes correspondantes. La méthode Java/Kotlin décrite utilise les bibliothèques natives officielles.
