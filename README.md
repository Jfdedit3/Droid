# Droid

Variante ARM64 de [DroidDeck 0.7.1](https://github.com/Droid-Deck/DroidDeck), adaptée au Samsung S22+ / Xclipse 920. Version des sources : **0.7.1-xclipse920.14**.

Le dépôt contient les sources complètes de l’application, les tests, les outils et les modifications. Les crédits et licences amont sont conservés ; la présentation originale est disponible dans [README.upstream.md](README.upstream.md).

Ubuntu/Plasma reçoit une apparence Windows claire : fond bleu original, menu à gauche, barre des tâches pleine largeur, applications épinglées et boutons de fenêtre à droite. Les réglages précédents sont sauvegardés et les personnalisations ultérieures sont conservées. **Fedora 43/Xfce garde sa configuration et son apparence de la .13.** Les choix RADV et GE-Proton sont conservés.

## État de validation

Six tests vérifient la sauvegarde, la restauration et l’absence de modification sur Fedora. Le script de disposition a été exercé avec un modèle des API Plasma (plusieurs écrans, applications épinglées et conservation des widgets du bureau). Les propriétés sont présentes dans les API Plasma 6.4 ; le fond a été inspecté. Les fichiers Fedora sont identiques à la version précédente. L’affichage complet sur S22+ reste à confirmer. Aucun gain de FPS n’est annoncé.

Les contrôles précédents des composants inchangés restent applicables. Fedora reste en aperçu ; le jeu et son rendu après le correctif de lanceur .13 doivent encore être confirmés sur S22+.

## Compiler

Voir [BUILD-XCLIPSE.fr.md](BUILD-XCLIPSE.fr.md) pour la procédure complète : JDK 17, Gradle 8.10.2, Android SDK 34 et build-tools 35.0.0. Le build recompile Java/Kotlin et récupère les bibliothèques natives inchangées depuis l’APK officiel 0.7.1. Les sources des composants natifs et leurs recettes restent disponibles dans le dépôt.

Préparer les entrées volumineuses du build :

- `fedora-xfce-arm64.tgz` : versionné en sept parties dans `build-inputs/fedora-base/`. Exécuter `python3 tools/fedora/assemble-base.py` pour reconstituer le fichier et vérifier son empreinte.
- `brave-arm64.deb` : paquet officiel Brave 1.99.57, URL et empreinte dans les instructions de compilation.

Le complément Fedora de jeu est inclus dans `app/src/main/assets/bundled/`. La clé privée de signature de l’APK distribué n’est pas incluse : configurer sa propre clé via `DROIDDECK_KEYSTORE` et `DROIDDECK_SIGNING_PASSWORD`.

## Télécharger et consulter les changements

[Page de téléchargement de l’APK](https://droiddeck-xclipse-download.z4trox.chatgpt.site) — accès selon les droits du site. [Notes techniques et historique](XCLIPSE-PERFORMANCE.fr.md).

Les workflows GitHub d’origine sont conservés dans `.github/upstream-workflows/`. Ils ne sont pas activés automatiquement dans ce dépôt et doivent être adaptés avant utilisation.

Pour reconstruire les composants natifs amont, initialiser les sous-modules épinglés avec `git submodule update --init --recursive`, puis suivre les recettes correspondantes. La méthode Java/Kotlin décrite utilise les bibliothèques natives officielles.
