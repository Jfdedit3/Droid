# Sources et compilation : DroidDeck Xclipse 920

Base : https://github.com/Droid-Deck/DroidDeck/archive/refs/tags/0.7.1.zip
Cette archive contient les fichiers modifiés complets à superposer à cette base, les tests, le profil documenté, les métadonnées publiques des composants et le script de récupération du runtime inchangé.

## Compilation

1. Cloner ce dépôt : les sources complètes de la base 0.7.1 et les modifications sont déjà présentes.
2. Installer un JDK 17, Gradle 8.10.2, Android SDK plateforme 34 et build-tools 35.0.0. Accepter les licences et configurer `sdk.dir` ou ANDROID_HOME.
3. Télécharger l'APK officiel : https://github.com/Droid-Deck/DroidDeck/releases/download/0.7.1/DroidDeck-0.7.1.apk
4. Vérifier son SHA-256 : 179ab8b4dc8f9223b70a1e4287c55a090b36ee376242186a60211100158462f1.
5. Depuis le dossier du projet : `python3 extract-official-runtime.py /chemin/DroidDeck-0.7.1.apk`.
6. Créer sa propre clé de signature PKCS12 avec l'alias `xclipse920`. Définir DROIDDECK_KEYSTORE vers ce fichier et DROIDDECK_SIGNING_PASSWORD vers son mot de passe. La clé privée utilisée pour le téléchargement n'est pas incluse.
7. Télécharger https://github.com/brave/brave-browser/releases/download/v1.99.57/brave-browser_1.99.57_arm64.deb, vérifier le SHA-256 d05df4f8a8c494075246820c17897a469614f879938a77557f025fda7be61db4 et placer le fichier dans app/src/main/assets/bundled/brave-arm64.deb.
8. Exécuter `python3 tools/fedora/assemble-base.py` pour reconstituer l’image Fedora depuis les sept parties incluses dans le dépôt. Le script vérifie les empreintes des parties et de l’image, puis place `fedora-xfce-arm64.tgz` dans `app/src/main/assets/bundled/`. Le complément de jeu et les manifestes sont déjà versionnés.
9. Exécuter `./gradlew --no-daemon --max-workers=3 :app:packageRelease -x verifyRuntimeInputs` avec JAVA_HOME vers le JDK 17.

Le build.gradle fourni recompile l'interface, Java et Kotlin ; il réutilise les bibliothèques natives et les assets de base de l'APK officiel 0.7.1. Le lanceur de bureau est remplacé par celui des sources et le paquet officiel Brave ARM64 ainsi que le système Fedora 43/Xfce ARM64 en aperçu sont ajoutés. Les tâches de recompilation native et la vérification des produits de cette recompilation ne s'appliquent donc pas à cette méthode et sont désactivées. Les empreintes et tailles des packs distants sont celles du BuildConfig officiel. Les licences d'origine sont conservées dans les assets.

Identifiant distinct : com.droiddeck.launcher.xclipse920. Version : 0.7.1-xclipse920.13. Les données de l'application officielle ne sont pas reprises automatiquement. Ce n'est pas une signature du projet officiel.

Les validations du bureau, FEX, journaux et JVM sont consignées dans les métadonnées et tools/fedora/validation.json. Le bureau a démarré sans droits root sous émulation ARM64. Steam y rencontre des appels de listes robustes non implémentés par QEMU ; son démarrage complet et les jeux sur le S22+ restent à vérifier. Ubuntu est conservé.
