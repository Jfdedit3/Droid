# Droid

Variante ARM64 de [DroidDeck 0.7.1](https://github.com/Droid-Deck/DroidDeck), adaptée au Samsung S22+ / Xclipse 920. Version des sources : **0.7.1-xclipse920.13**.

Le dépôt contient les sources complètes de l’application, les tests, les outils et les modifications. Les crédits et licences amont sont conservés ; la présentation originale est disponible dans [README.upstream.md](README.upstream.md).

Ubuntu/Plasma est conservé. Fedora 43/Xfce est un second bureau en aperçu avec Steam et les composants de jeu partagés, Brave et un profil distinct. Cette version ajoute l’utilitaire gamescopereaper ARM64 manquant et limite le réglage logiciel X11 au bureau. Les réglages RADV et GE-Proton choisis sont conservés.

## État de validation

15 tests Java, 26 tests Fedora et 10 tests des lanceurs GPU passent. Le complément réel a été installé avec le code de l’application. L’utilitaire ARM64 a lancé et attendu son enfant sous émulation ; un second essai vérifie son arrêt propre. Le jeu et son rendu sur S22+ après ce correctif restent à confirmer. Aucun gain de FPS n’est annoncé.

## Compiler

Voir [BUILD-XCLIPSE.fr.md](BUILD-XCLIPSE.fr.md) pour la procédure complète : JDK 17, Gradle 8.10.2, Android SDK 34 et build-tools 35.0.0. Le build recompile Java/Kotlin et récupère les bibliothèques natives inchangées depuis l’APK officiel 0.7.1. Les sources des composants natifs et leurs recettes restent disponibles dans le dépôt.

Les deux entrées volumineuses ne sont pas stockées dans Git :

- `fedora-xfce-arm64.tgz` : pièce jointe de la release `v0.7.1-xclipse920.13` de ce dépôt, avec son empreinte.
- `brave-arm64.deb` : paquet officiel Brave 1.99.57, URL et empreinte dans les instructions de compilation.

Le complément Fedora de jeu est inclus dans `app/src/main/assets/bundled/`. La clé privée de signature de l’APK distribué n’est pas incluse : configurer sa propre clé via `DROIDDECK_KEYSTORE` et `DROIDDECK_SIGNING_PASSWORD`.

## Télécharger et consulter les changements

[Page de téléchargement de l’APK](https://droiddeck-xclipse-download.z4trox.chatgpt.site) — accès selon les droits du site. [Notes techniques et historique](XCLIPSE-PERFORMANCE.fr.md).

Les workflows GitHub d’origine sont conservés dans `.github/upstream-workflows/`. Ils ne sont pas activés automatiquement dans ce dépôt et doivent être adaptés avant utilisation.
