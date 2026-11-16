## Version 0.7.1-xclipse920.14 — apparence Windows pour Ubuntu uniquement

Le lanceur Ubuntu/Plasma prépare une apparence Windows claire : fond bleu original, bouton de menu à gauche, barre des tâches pleine largeur en bas, applications épinglées (Fichiers, Brave, Steam, Terminal), zone de notification et date à droite. Les boutons de fenêtre sont à droite ; les dossiers utilisent le double clic. La barre est opaque et le flou est désactivé. Les réglages de rendu et de jeux restent ceux déjà choisis ; aucun gain de FPS n’est mesuré.

Le profil vérifie ID=ubuntu avant toute écriture. Il est appelé uniquement par le lanceur Plasma Ubuntu. Les lanceurs Fedora, son installateur et son complément de jeu sont identiques à la .13. Les fichiers originaux de configuration Ubuntu sont sauvegardés dans ~/.local/share/droiddeck/ubuntu-windows-v1/. Le profil ne se réapplique pas sur les personnalisations effectuées ensuite. Le panneau est reconstruit une fois par un script de mise à jour Plasma, après chargement des bureaux ; les widgets du bureau et les fichiers personnels sont conservés. Les anciens widgets du panneau restent récupérables avec la sauvegarde.

Pour revenir à l’apparence d’origine : fermer le bureau Ubuntu, ouvrir un terminal dans le système Ubuntu, exécuter `python3 /usr/local/bin/droiddeck-ubuntu-windows --restore`, puis relancer le bureau. Cette restauration remet les configurations sauvegardées et ne réactive pas automatiquement le nouveau profil.

Validation : six tests Python couvrent sauvegarde, personnalisation ultérieure, restauration, liens symboliques, refus sur Fedora et échec sans modification si les assets manquent. Le script de disposition Plasma a été exercé avec un modèle de ses API (plusieurs écrans, applications épinglées, conservation des widgets du bureau). Les propriétés utilisées ont été vérifiées dans les API Plasma 6.4. Le fond a été inspecté. L’affichage complet sur le S22+ reste à confirmer.

## Version 0.7.1-xclipse920.13 — utilitaire de suivi des jeux Fedora

Le rapport du S22+ confirme le chargement des bibliothèques de la .12, la sélection RADV et le démarrage de l’affichage gamescope. Le lancement s’arrête ensuite sur « Failed to start process gamescopereaper: No such file or directory ». Le complément embarqué ajoute le véritable gamescopereaper ARM64 du paquet Fedora gamescope-3.16.23-1.fc43, vérifié par signature GPG et SHA-256, avec sa licence. Le programme gamescope personnalisé 3.16.29 reste inchangé.

Le complément se met à jour automatiquement au prochain lancement du bureau existant. Le marqueur de préparation vérifie maintenant aussi que l’utilitaire est présent et exécutable. La désactivation de Glamor reste limitée au serveur X11 du bureau : elle n’est plus transmise au serveur X11 des jeux. Les choix RADV et GE-Proton restent conservés.

Validation : 15 tests Java passent (7 extraction, 8 complément), 26 tests Fedora et 10 tests des lanceurs GPU passent. Le complément réel a été extrait avec le code de l’application. Son utilitaire ARM64 a réellement lancé et attendu un processus enfant sous QEMU, puis un second essai a vérifié sa fermeture lors d’un arrêt du lanceur. Le jeu et son rendu sur S22+ après cette correction restent à confirmer. Aucun gain de performances n’est mesuré ; Fedora reste en aperçu.

## Version 0.7.1-xclipse920.12 — dépendances du lanceur de jeux Fedora

Le nouveau rapport du S22+ confirme que Steam atteint la bibliothèque et lance Kaiju No. 8, mais gamescope s’arrête avant le jeu : libwayland-server.so.0 manque. L’audit trouve aussi libseat, libinput, libavif, libeis et LuaJIT absents. Un essai réel du binaire ARM64 montre ensuite que la bibliothèque C++ de Fedora ne fournit pas GLIBCXX_3.4.35 demandé par ce gamescope compilé avec GCC 16.

Un complément local de 2,1 Mo compressés ajoute les six bibliothèques et leurs dépendances (11 paquets Fedora signés, sans mise à niveau des paquets existants). La bibliothèque C++ ARM64 Ubuntu 26.04, compatible avec glibc >= 2.38, fournit l’ABI manquante ; le fichier de bibliothèque Fedora d’origine est conservé et le lien libstdc++.so.6 pointe vers la nouvelle bibliothèque. Le complément contient les licences et les données libinput/libwacom. Son empreinte figure dans tools/fedora/gaming-manifest.json.

Le complément est vérifié et appliqué automatiquement au lancement du bureau existant, sans réinstaller Fedora ni modifier son dossier personnel. Les démarrages suivants évitent son extraction si les fichiers et le marqueur sont présents. La bibliothèque C++ est choisie aussi pour les programmes natifs de jeu qui utilisent le runtime système partagé.

Validation : 14 tests Java passent (7 pour l’extraction, 7 pour le complément : conservation des données, refus de chemins ou liens invalides, corruption, installation incomplète, réparation et démarrage sans réextraction). Le complément réel a été appliqué avec le code Java de l’application, puis gamescope ARM64 3.16.29 a démarré avec --version sous QEMU 10.1.5 et les bibliothèques Fedora. Cela valide son chargement et ses dépendances, pas un rendu Vulkan ni le jeu sur S22+. Les versions de Xfce testées sans affichage atteignent leur initialisation GTK, mais aucun bureau complet après changement de bibliothèque C++ n’a été validé ici. Fedora reste en aperçu.

## Version 0.7.1-xclipse920.11 — transport local Steam dans Fedora

Le rapport téléphone suivant confirme la disparition des échecs Zink et ICE, mais Steam affiche 0x3009, échoue à connecter son websocket et répète « lsof: command not found ». Fedora reçoit désormais le même chargement système de libblsession/libfakeinput qu’Ubuntu : il reste actif quand Steam efface LD_PRELOAD dans ses enfants. Un adaptateur limité aux requêtes TCP loopback de Steam lit les enregistrements de sockets produits par libblsession et fournit le même protocole lsof que le correctif officiel ; il ne remplace pas un lsof déjà installé et ne prétend pas être un lsof généraliste. Les préchargements personnalisés sont conservés.

Validation : 26 tests Fedora et un test natif avec une vraie socket loopback vérifient le propriétaire retourné à un enfant ayant effacé LD_PRELOAD. Les contrôles de connexion et le garde du port de débogage sont conservés. L’erreur observée a été analysée, mais la connexion complète de Steam après correction n’a pas été validée sur S22+ ; Fedora reste en aperçu. Mise à jour sur l’installation existante, sans réinstaller Fedora.

## Version 0.7.1-xclipse920.10 — correctifs Fedora issus du rapport téléphone

Le rapport de session Fedora du 16 novembre montre des échecs répétés Zink (`vkEnumeratePhysicalDevices`), puis `VK_ERROR_DEVICE_LOST` dans Steam, un verrou ICE impossible à créer et un module de certificats NSS introuvable. Cette version force llvmpipe pour le bureau et l’interface Steam, et rétablit les valeurs OpenGL initiales dans les lanceurs de jeux compatibles. Le pilote Vulkan RADV et GE-Proton choisis restent conservés. Le rendu logiciel peut consommer du CPU ; aucun gain de FPS n’est mesuré.

Un lanceur iceauth limité à Fedora utilise le correctif de verrou déjà embarqué ; un verrou existant reste protégé. Steam retrouve le module de confiance p11-kit de Fedora sous le nom libnssckbi.so, sans désactiver la vérification des certificats. Les erreurs précoces de préparation Steam sont désormais enregistrées. Des étapes horodatées distinguent X11 et Xfce dans le prochain rapport.

Validation : 20 tests Fedora, 10 tests des lanceurs GPU, 157 tests d’environnement des jeux. Le véritable iceauth du poste de test fonctionne sous un filtre interdisant link/linkat avec le correctif ; sans lui l’échec du journal est reproduit, et un verrou actif reste préservé. Ces tests ne remplacent pas une validation Steam sur le S22+. Fedora reste en aperçu, le démarrage complet et les performances après correction doivent être confirmés sur le téléphone. La base Fedora embarquée ne change pas et ne nécessite pas une réinstallation du système.

# Version courante : 0.7.1-xclipse920.9

Fedora 43 ARM64/Xfce remplace Debian dans le choix du second bureau. Il est proposé en aperçu : le bureau a démarré sous émulation ARM64 sans droits root, mais le démarrage complet de Steam et les jeux sur le S22+ ne sont pas validés. Le client Steam est lancé dans Fedora, sans fermer le bureau ni ouvrir Ubuntu. Ses fichiers, jeux, Proton, FEX et Brave sont partagés pour éviter une seconde copie ; Fedora conserve son propre système et son propre dossier personnel.

Ubuntu/Plasma et les choix existants de RADV/GE-Proton sont conservés. L'ancien Debian n'est pas effacé automatiquement : un bouton permet sa suppression explicite avec ses fichiers personnels. Son archive n'est plus intégrée à l'APK. Fedora désactive la composition Xfce et l'agent PolicyKit qui ne peut pas obtenir de bus système Android. Brave et le protocole steam:// utilisent les raccourcis du bureau courant.

Les montages communs utilisent le dossier du runtime plutôt que des centaines de fichiers individuels. Les environnements FEX utilisent la racine active et séparent les bibliothèques ARM Fedora de celles du système x86. Une erreur du client Steam conserve aussi ses journaux lorsque le bureau se termine avec succès.

Validation : 17 tests Python du bureau et de son intégration, 9 tests Python FEX/bubblewrap et 9 tests isolés de conservation des journaux. Les contrôles JVM et l'extraction réelle de l'archive sont consignés dans les métadonnées de compilation. Le serveur Xwayland ARM64 a été vérifié avec des appels de création de liens refusés ; 243 fichiers ELF ARM64 du client Steam ont été contrôlés sans dépendance manquante. Son navigateur intégré échoue sous QEMU sur les listes de verrous robustes : get_robust_list et process_vm_readv y retournent ENOSYS. Aucune mesure de performances GPU sur téléphone n'est revendiquée.

La recette, les versions de paquets, le manifeste et la capture du bureau de test sont dans tools/fedora/. Les notes suivantes décrivent les versions antérieures.

---

# DroidDeck 0.7.1 — profil expérimental Xclipse 920

Cette variante applique automatiquement un profil à l'Exynos 2200 / Xclipse 920 au premier démarrage. Elle configure 54 préférences de session et 3 préférences de génération d'images, y compris lorsqu'une ancienne valeur existe. Beaucoup de valeurs confirment les bons réglages déjà présents dans DroidDeck ; elles ne constituent pas 57 nouvelles optimisations.

Ce profil privilégie un faible coût graphique et une compatibilité raisonnable. Il n'a pas été mesuré sur un Galaxy S22+. Aucun réglage universel ne maximise les performances de tous les jeux.

## Réglages retenus et raisons

| Domaine | Réglage | Raison et compromis |
| --- | --- | --- |
| Résolution | Steam et mode run : 540p ; bureau : 720p | Sur le S22+ 2340 × 1080, rendu 1170 × 540 au lieu de 1560 × 720 : 43,75 % de pixels en moins. Image moins détaillée. Ce pourcentage n'est pas un gain de FPS mesuré. |
| Mise à l'échelle | Linéaire ; post-traitements désactivés | Réduit les traitements supplémentaires : pas de FSR, NIS, netteté CAS, FXAA, CRT, debanding ou simulation HDR. |
| Textures | Aucun filtrage anisotrope forcé ; biais LOD nul | Évite d'ajouter du filtrage ou de forcer des textures plus détaillées. Le jeu conserve son propre filtrage anisotrope. |
| Pilote | Choix automatique à la première installation ; choix ultérieurs conservés | La paire RADV déjà choisie dans la variante existante reste sélectionnée. |
| Vulkan | Couche Xclipse et DXVK 2.6.2 existants conservés | Le chemin système ne peut pas fournir les 256 octets de push constants exigés par DXVK 3. Pas de faux déblocage de fonctions matérielles. Venus et le bridge Mali sont désactivés. |
| Interface Steam | Composition GPU des pages désactivée | Conserve le contournement des tuiles corrompues documenté par le projet. La composition GPU pourrait être plus rapide mais reste à vérifier sur le téléphone. |
| Traduction x86 | FEX PERFORMANCE_TSO | Multibloc et précision x87 réduite ; conservation du TSO principal. Pas de mode Extreme désactivant les contrôles du code automodifiant. Peut nécessiter le mode Compatibilité pour certains jeux. |
| Synchronisation | NTSYNC avec repli activé, pas de priorité FSYNC | Garde la voie prévue par le projet et son repli ; le noyau détermine les fonctions réellement disponibles. |
| CPU | Indications de threads actives ; aucune affinité forcée | L'ordonnanceur peut employer tous les cœurs disponibles ; pas de répartition rigide supposée idéale sans mesures. |
| PRoot | Fast path actif ; seccomp conservé | Garde l'accélération existante et son mécanisme de filtrage. |
| OpenGL/Zink | Thread GL, absence de validation GL, Zink lazy actifs | Conserve les optimisations de la traduction GL. Ces variables n'accélèrent pas toutes les applications ni le pilote Vulkan Samsung lui-même. |
| Mémoire | RAM/VRAM automatiques ; client zRAM actif | Évite de réserver une quantité arbitraire de mémoire unifiée. Le client zRAM n'implique pas une reconfiguration du zRAM Android. |
| Audio | Audio direct actif | Utilise le chemin existant si le pack et le périphérique le permettent ; le projet conserve ses conditions de disponibilité. |
| Entrées | Faible latence active | Conserve la transmission des entrées et le traitement tactile sans mise en tampon lorsque le système le permet. |
| Affichage | Vsync thread et rendu sur commit actifs | Conserve les mécanismes existants de présentation. Le profil ne force ni plafond FPS ni fréquence de l'écran. |
| Surcouches et interface | MangoApp, HUD, télémétrie automatique et animations désactivés | Évite ces affichages et traitements supplémentaires ; réactiver le HUD manuellement pour les mesures. |
| Génération d'images | Désactivée | Évite la charge d'interpolation et sa latence ; priorité aux images réellement calculées. |
| Diagnostic | Journaux ordinaires, debug des jeux, détails GPU et diagnostics stockage/VAC désactivés | Réduit la journalisation facultative ; la réactiver pour diagnostiquer un jeu qui ne démarre pas. Le gestionnaire de crash reste présent. |
| Arrière-plan | Option de téléchargements Steam en arrière-plan désactivée | Ne conserve pas une session seulement pour cette option ; ne bloque pas tous les téléchargements pendant une partie. |
| Fréquences et chauffe | Pas d'épinglage de fréquence GPU ni de priorité temps réel du compositeur | Ne suppose pas que ces options améliorent les performances soutenues du S22+. La gestion thermique Samsung reste active. |

## Application et restauration

Le profil est appliqué dans `App.onCreate`, après les valeurs NTSYNC initiales et avant le démarrage des services principaux. La détection exige le HAL Xclipse et l'identification Exynos 2200, S5E9925 ou Xclipse 920. Un S22+ Snapdragon ou un Xclipse 940/950 ne reçoit pas ce profil.

Une sauvegarde des seules clés concernées est enregistrée dans les préférences privées `xclipse920-backup-session` et `xclipse920-backup-frame-gen`, avant modification. Si la sauvegarde échoue, le profil n'est pas appliqué. Il ne se réapplique pas à chaque lancement : les ajustements manuels ultérieurs restent possibles.

Dans les paramètres de session, le groupe « Profil performances Xclipse 920 » propose « Restaurer les anciens réglages ». Arrêter la session avant de restaurer. Fermer puis rouvrir les paramètres après restauration. Les valeurs restaurées seront utilisées à la prochaine session. Le profil ne se réappliquera pas automatiquement après restauration.

Les comptes, fichiers, contrôles et profils propres à chaque jeu ne sont pas modifiés. Un profil de jeu ou des options internes au jeu peuvent donc remplacer une partie des réglages globaux. Choisir les options graphiques du jeu sur Faible pour commencer, puis les ajuster selon les mesures.

L'adaptation précédente est conservée : choix 540p disponible dans le menu ; valeur implicite 540p sur Xclipse pour steam/run en l'absence de réglage enregistré. Restaurer une valeur absente revient à cette valeur implicite de cette variante, pas nécessairement à celle du binaire officiel.

## Livraison et vérification

Une variante APK release est préparée sous le nom DroidDeck Xclipse 920 et l'identifiant com.droiddeck.launcher.xclipse920. Elle se compile avec un JDK 17 et l'Android SDK 34 ; les bibliothèques natives et les assets de runtime sont repris de l'APK officiel 0.7.1. Voir BUILD-XCLIPSE.fr.md pour reproduire cette méthode et ses choix de signature.

- 19 tests de géométrie/résolution exécutés isolément avec Kotlin 2.1.0 et JUnit.
- 6 tests du profil : détection, application unique, conservation des modifications ultérieures et des données non concernées, restauration, échec de sauvegarde et nouvelle tentative après échec d'écriture.
- Ces tests isolés utilisent des substituts minimaux pour les types Android et des préférences en mémoire ; ils ne valident pas le démarrage réel sous Android ni l'interface Compose.
- Ressources XML anglaises et françaises vérifiées ; clés du profil rapprochées des préférences existantes.
- La configuration du proxy Java a permis de lever le blocage initial de Gradle. La tâche Android :app:packageRelease a réussi ; signature, identifiant, appel du profil au démarrage, 35 bibliothèques natives et 204 assets de runtime ont été vérifiés dans l'APK. Les profils ART sont régénérés. L'analyse lint globale n'a pas été menée à son terme. Les tests unitaires restent les exécutions isolées décrites ci-dessus. Aucun essai matériel n'a été réalisé.

Pour mesurer les gains, comparer le même jeu, la même scène, le même pilote et la même limite FPS en 720p puis 540p. Relever les FPS et les à-coups après quelques minutes puis après 15 minutes. Une limitation CPU ou une incompatibilité du pilote peut annuler le gain attendu de la baisse de résolution.

## Liste exacte des préférences écrites


### session

| Clé | Valeur |
| --- | --- |
| `displayResolution.steam` | `"540p"` |
| `displayResolution.run` | `"540p"` |
| `displayResolution.lxqt` | `"720p"` |
| `hdr.steam` | `false` |
| `hdr.run` | `false` |
| `hdr.lxqt` | `false` |
| `upscaler` | `0` |
| `screenEffects` | `"{}"` |
| `textureAnisotropy` | `0` |
| `textureLodBias` | `"0"` |
| `gpuDriverMode` | `"auto"` |
| `androidDriver` | `""` |
| `linuxDriver` | `""` |
| `linuxDriver.steam` | `""` |
| `linuxDriver.desktop` | `""` |
| `vkBridge` | `false` |
| `venus` | `false` |
| `steamGpuCompositing` | `false` |
| `fexPreset` | `"PERFORMANCE_TSO"` |
| `forceSsbs` | `false` |
| `fastSync` | `true` |
| `fsyncFirst` | `false` |
| `syncFallback` | `true` |
| `prootFastPath` | `true` |
| `prootNoSeccomp` | `false` |
| `clientCpusOverride` | `false` |
| `clientCpus` | `""` |
| `gameCpus` | `""` |
| `gameThreadHints` | `true` |
| `glThread` | `true` |
| `noGlError` | `true` |
| `zinkLazy` | `true` |
| `gamescopeRealtime` | `false` |
| `gpuClockPin` | `false` |
| `ramCapMb` | `0` |
| `vramCapMb` | `0` |
| `zramClient` | `true` |
| `directAudio` | `true` |
| `lowLatency` | `true` |
| `mangoapp` | `false` |
| `hud` | `false` |
| `hudDeck` | `false` |
| `bottomHud` | `false` |
| `memoryHud` | `"off"` |
| `telemetryAuto` | `false` |
| `animations` | `false` |
| `storageDiagnostics` | `false` |
| `vac_diagnostics` | `false` |
| `logs` | `false` |
| `gameDebugLogsEnabled` | `false` |
| `detailedGraphicsLogs` | `false` |
| `steamDownloadsInBackground` | `false` |
| `vsyncThread` | `true` |
| `renderOnCommit` | `true` |

### frame_gen

| Clé | Valeur |
| --- | --- |
| `engine` | `"off"` |
| `target` | `0` |
| `multiplier` | `2` |


## Version 0.7.1-xclipse920.2 — composants d'installation

- Le téléchargeur des composants vérifiés par SHA-256 peut abandonner une source restant sous 128 Kio/s pendant une fenêtre de 20 secondes, lorsqu'il existe une autre source et qu'il reste plus de 8 Mio. Une source ne peut plus retenir indéfiniment le transfert simplement en envoyant quelques octets.
- La dernière source est autorisée à finir, même si le réseau entier est lent. Les petits transferts et ceux de taille inconnue ne sont pas basculés selon ce critère.
- La reprise avec ETag, l'annulation, la validation SHA-256 et la restriction à la source originale sans empreinte sont conservées. Changer de source recommence le fichier ; la bascule évite donc les fichiers presque terminés. Les URL et métadonnées d'autorité restent directes.
- La progression est limitée à quatre notifications par seconde quand les données arrivent, au lieu d'attendre chaque mégaoctet. Le signal 100 % n'est émis qu'après validation finale du téléchargement.
- 22 tests du téléchargeur passent : reprise, annulation, changement de source lent simulé sans attente réelle, dernier serveur lent, ralentissement après un bon démarrage, intégrité et progression. Ils utilisent de vrais transferts HTTP locaux, avec uniquement la journalisation Android substituée et une horloge injectée pour les scénarios de débit.
- Cette version conserve le même identifiant et la même clé de signature que la variante .1, avec le code de version 20. Elle s'installe par-dessus .1 sans réinitialiser ses données. Le profil de performances n'est pas réappliqué aux installations où il a déjà été configuré.

Le débit sur le réseau du téléphone n'a pas été mesuré. En mode de source Directe, aucune source alternative n'est utilisée. Pour les composants GitHub vérifiés, sélectionner Automatique ou choisir un Miroir dans les paramètres de téléchargement. Cette modification concerne le téléchargeur Android des composants ; elle n'accélère pas le réseau propre à Steam ou aux jeux.


## Brave dans le bureau Linux

Brave stable 1.99.57 ARM64 est inclus dans l'APK, issu du paquet officiel `brave-browser_1.99.57_arm64.deb` (SHA-256 : `d05df4f8a8c494075246820c17897a469614f879938a77557f025fda7be61db4`). Aucun téléchargement supplémentaire de Brave n'est nécessaire. Le paquet est installé lors de la préparation d'une session de bureau Linux, afin de ne pas ajouter cette extraction au lancement de Steam ou d'un jeu. Il ajoute environ 145 Mo à l'APK et nécessite environ 500 Mo dans le runtime Linux.

Le paquet est lu en streaming. Seul son répertoire `/opt/brave.com/brave` est extrait, dans un dossier temporaire, puis activé par renommage ; aucune commande de maintenance Debian n'est exécutée sur le runtime Ubuntu. Les licences fournies avec Brave sont conservées. Les fichiers HTML et les liens HTTP/HTTPS ainsi que le navigateur préféré de KDE passent à Brave lors du premier démarrage du bureau après installation. Les choix ultérieurs de navigateur sont conservés. Firefox et ses profils ne sont pas effacés.

Le lanceur utilise le wrapper Brave officiel, X11 et les options de compatibilité Chromium de cet environnement, dont `--no-sandbox`. L'isolation de Chromium est donc réduite comme pour les autres lanceurs Chromium de ce bureau. La compatibilité au lancement sur un S22+ n'a pas été vérifiée. Les dépendances partagées restent celles du pack de bureau. Brave est mis à jour par une nouvelle version de cet APK, sans gestion de paquets Debian ajoutée au runtime Ubuntu.

Tests : 3 tests d'extraction synthétique (permissions, confinement des chemins, paquet incomplet), extraction du paquet ARM64 officiel avec vérification de l'exécutable, de la langue française et de la licence, et 3 tests des associations du navigateur (conservation des autres associations et des choix ultérieurs). Ces tests s'ajoutent aux 47 tests précédents.


## Diagnostic du démarrage des jeux

Le propriétaire du S22+ a confirmé que le jeu fonctionne après avoir remplacé le Proton expérimental par GE-Proton proposé par l'application. La cause du blocage n'est donc pas attribuée au pilote RADV sur la seule base de l'écran de chargement. Les modifications préparées pour imposer une autre paire RADV ou réinitialiser les paramètres de compatibilité n'ont pas été livrées.

Cette mise à jour conserve la paire RADV et GE-Proton déjà sélectionnés : le profil est appliqué une seule fois et ne touche pas au choix du composant Proton. Sur une nouvelle installation, le mode de pilotes initial est Automatique au lieu d'imposer le pilote système. Sur une installation déjà configurée, les réglages de pilotes restent ceux de l'utilisateur.

## Version 0.7.1-xclipse920.3 — FG léger ×2 · Auto

Le menu « Génération d’images » propose un nouveau mode de pilotage du moteur DD-FG existant. Ce n'est pas un troisième algorithme d'interpolation : il réutilise les shaders natifs intégrés, avec le modèle 3, le préréglage performance 2 et une image intermédiaire au maximum. Aucune bibliothèque native, DLL Lossless Scaling ou donnée de modèle supplémentaire n'est téléchargée. L'échelle de mouvement reste celle de DD-FG standard ; elle n'est pas présentée comme une réduction de résolution.

Ce mode reste désactivé jusqu'à sa sélection manuelle. Un contrôleur Android mesure une fois par seconde le nombre d'images source reçues par le compositeur, sans compter les images synthétiques. Trois mesures stables sont requises avant activation : au moins 24 images source/s et au plus 55 % de la fréquence de présentation. Lorsqu'il est actif, il tolère une plage plus large pour limiter les bascules et se suspend sous 18 images/s ou à partir de 75 % de la fréquence d'affichage.

Il se suspend en mode économie d'énergie et dès l'état thermique Android « modéré » (ou plus élevé). Après une chauffe, il attend au moins 15 secondes sans cet état avant une nouvelle stabilisation. Une baisse de plus de 20 % du débit source par rapport à l'activation déclenche une pause de 30 secondes. Ce signal est une heuristique de surcharge ; il ne prouve pas que l'interpolation est responsable de la baisse. Les changements de moteur et la mise en arrière-plan arrêtent la surveillance. Une erreur de capacité du pilote désarme la génération et affiche un message ; le jeu continue sans interpolation.

L'indicateur de débit interpolé est masqué lorsque ce mode est suspendu, pour éviter d'afficher un ancien compteur de génération comme s'il était encore actif. La télémétrie de source est celle du compositeur, pas une mesure du temps de simulation du moteur du jeu.

La génération d'images ajoute une latence de présentation et peut créer des artefacts. Elle n'augmente pas les FPS réellement calculés par le jeu. Le choix du modèle 3 et la limitation ×2 privilégient une charge contenue, mais aucun gain ni économie GPU n'est garanti sans mesure sur le S22+.

Validation : 8 tests de politique (stabilisation, plages de fréquence, hystérésis, chauffe, économie d'énergie, refroidissement, chute de débit et mesures invalides) et 4 tests isolés de préférences/liaison (limitation ×2, données enregistrées incohérentes, retour au DD-FG standard et suspension sans effacer le choix). Ils complètent les 53 tests ciblés de la version précédente. Les tests de liaison utilisent des substituts Android et natifs ; ils ne constituent pas un essai GPU. La bibliothèque native est identique à celle de la version précédente. RADV, GE-Proton, Brave et les données de l'utilisateur sont conservés.


## Version .4 : deux distributions de bureau

Deux choix apparaissent dans l’onglet Bureau : Ubuntu/Plasma existant, et Debian 13/Xfce 4.20 léger. Debian utilise un système ARM64 distinct, inclus dans l’APK ; son installation locale exige 2 Go libres et vérifie le SHA-256 avant extraction. Aucun composant Debian n’est téléchargé au premier démarrage. Le système et les fichiers personnels d’Ubuntu restent séparés de Debian. Le bouton Supprimer Debian demande confirmation et ne concerne que cette seconde installation.

Le nouveau bureau utilise Xwayland rootful en mémoire partagée, sans Glamor ; la composition Xfwm est désactivée, un seul espace de travail est configuré et la sauvegarde automatique de session est désactivée. Ces choix réduisent les effets graphiques et la charge GPU, sans promettre de gain mesuré sur Xclipse 920. L’affichage reprend la résolution de bureau (720p par défaut du profil). Les réglages personnels modifiés ensuite ne sont pas réécrits.

Brave est le navigateur initial de Xfce. Son programme ARM64 est partagé avec Ubuntu pour éviter une nouvelle copie de plusieurs centaines de Mo ; les profils restent distincts. Les jeux passent par Ubuntu avec les pilotes et Proton déjà sélectionnés. Debian ne reçoit pas de copie des réglages RADV/Proton et ne remplace pas le runtime de Steam.

Le terminal Debian fonctionne avec l’UID de l’application, sans privilèges réels et sans faux UID 0. Les services système complets et l’installation de paquets demandant un administrateur ne sont pas intégrés au second bureau.

Validation : 7 tests Java d’extraction et 4 tests du lanceur ; démarrage réel sous QEMU ARM64/PRoot de Xwayland, xfce4-session, xfwm4, xfce4-panel, Thunar et xfdesktop sur un serveur Wayland Weston/Pixman. La présence du gestionnaire de fenêtres et des fenêtres X11 a été vérifiée. Ce test ne valide pas l’affichage Android, le son, le clavier ou Brave sur le S22+. Voir tools/debian pour les versions et la recette du système.

Le binaire Brave ARM64 partagé a également exécuté `--version` sous Debian émulé, ce qui vérifie le chargement de ses bibliothèques ; cela ne valide pas une navigation complète.

## Version .5 : rapports d’échec conservés

Les sessions qui échouent conservent leurs journaux temporaires même lorsque la conservation normale est désactivée. Les trois derniers dossiers d’échec restent accessibles à l’écran de fin et dans la liste des journaux ; les sessions réussies restent supprimées lorsque les journaux sont désactivés. Le statut de sortie est transmis à la collecte sans dépendre de l’état d’une session suivante.

Pour Debian, la sortie précoce de PRoot est capturée dans session.log. Le lanceur écrit ses diagnostics dès son entrée, conserve le stderr de Xwayland dans xwayland-debian.log, indique son statut réel en cas d’arrêt et attend la présence du socket Wayland avant de lancer le serveur X11. Le délai X11 est porté à 30 secondes ; une expiration inclut le message de xdpyinfo. Le lanceur est actualisé à chaque démarrage, y compris pour un Debian déjà installé par la version .4 ; aucune réinstallation du système Debian n’est nécessaire.

La capture d’écran reçue signale un code 70. Elle ne permet pas d’identifier la cause exacte de l’arrêt de Xwayland sur le téléphone sans son stderr. Cette version permet de recueillir ce diagnostic ; elle ne constitue pas une validation du correctif graphique sur S22+.

Validation supplémentaire : 8 tests des chemins et de la conservation des rapports, dont 4 régressions sur les sessions en échec, la limite de trois dossiers et une nouvelle session démarrée pendant la collecte ; 4 tests du lanceur Debian repassés, avec contrôle du statut Xwayland et de son stderr.

## Version .6 : verrou Xwayland identifié dans le rapport du téléphone

Le rapport de session 2026-11-16-07-desktop contient : `Linking lock file (/tmp/.X1-lock) in place failed: Operation not permitted`. Xwayland s’arrête avant Xfce. Cette erreur de création de lien intervient avant le rendu du bureau.

Les commandes du bureau Debian et de son terminal utilisent maintenant l’option PRoot `--link2symlink`, un répertoire privé `debian-xfce/.proot-l2s` et `PROOT_NO_SECCOMP=1`. Le traçage complet permet d’intercepter l’appel avant une éventuelle interdiction par le filtre du noyau. Cette option peut coûter plus de CPU sur les appels système ; elle est limitée au bureau Debian. Les réglages PRoot des sessions Ubuntu, Steam et des jeux restent ceux de l’utilisateur.

Trois tests Linux x86-64 reproduisent une interdiction des liens par un filtre seccomp : sans compatibilité le verrou échoue avec EPERM ; avec l’accélération seccomp il échoue aussi ; avec l’émulation des liens et le traçage complet, sa création, son compteur de liens et la lecture après suppression du nom temporaire réussissent dans un système de fichiers isolé. Le test utilise le commit Termux/PRoot 4dba3afbf3a63af89b4d9c1a59bf2bda10f4d10f, référencé par tools/proot/source.env ; le binaire Android officiel n’est pas recompilé.

Reproduire : `python3 tools/tests/run_debian_link_tests.py --proot /chemin/vers/proot-termux-pour-linux`. Fournir si nécessaire LD_LIBRARY_PATH vers libtalloc. Les sources des deux sondes C sont incluses dans tools/tests/debian_link_compat. Ce test vérifie le mécanisme qui échouait, pas l’ensemble du bureau sur le S22+ ; un nouvel essai sur l’appareil reste nécessaire.

## Version .7 : retrait du traçage forcé et compatibilité locale à Xwayland

Le rapport suivant, 2026-11-16-08-desktop, montre une régression de la .6 : `getcwd() failed: Function not implemented` puis `mkdir ... Function not implemented`, avant le démarrage de Xwayland. Le test précédent sur PRoot Linux ne validait pas le comportement du PRoot Android sur ce téléphone. Les options `--link2symlink`, `PROOT_L2S_DIR` et le forçage `PROOT_NO_SECCOMP=1` ajoutés à Debian sont retirés. Les préférences PRoot de l’utilisateur sont à nouveau utilisées normalement.

Le correctif du verrou utilise désormais une fonction déjà présente dans le libblsession.so ARM64 officiel inchangé. Le programme Android copie ce fichier dans Debian sous usr/local/lib/droiddeck-xwayland.so, puis le lanceur active LD_PRELOAD et DROIDDECK_LINK_RENAME=1 uniquement pour Xwayland et ses éventuels sous-processus. Xfce, Brave et les jeux ne reçoivent pas ces variables. Si le noyau refuse le lien, le mécanisme existant utilise renameat2(RENAME_NOREPLACE), ce qui respecte un verrou préexistant. Le code LockServer de Xwayland 24.1.6 supprime ensuite le nom temporaire sans exiger qu’il existe encore.

Cinq nouveaux tests portent sur le véritable binaire Debian Xwayland ARM64, exécuté sous QEMU 10.0.13 avec ses bibliothèques Debian et le libblsession.so exact de l’APK. Un filtre seccomp du noyau refuse link et linkat : sans préchargement le message original EPERM est reproduit ; sans activation explicite du mécanisme le message est aussi reproduit ; avec le correctif le serveur X11 démarre sous Weston/Pixman et xdpyinfo confirme 1280×720 ; un verrou d’un processus actif reste intact ; le démarrage fonctionne également sans interdiction des liens. Le test n’utilise aucun traçage PRoot forcé.

Le test des quatre scénarios du lanceur confirme aussi que les variables de compatibilité sont présentes pour Xwayland et absentes de la session Xfce. Voir tools/tests/test_debian_xwayland_arm64.py. Il nécessite QEMU user ARM64, le binaire Xwayland et les bibliothèques extraits du système Debian inclus, un Weston headless Linux x86-64 et xdpyinfo. Le diagnostic du téléphone n’est ni inclus dans l’APK ni publié dans les sources. Le test logiciel ARM64 ne remplace toujours pas la vérification sur le S22+ et son compositeur Android.

## Version .8 : fond de bureau et accès aux applications

Le bureau Debian/Xfce démarre sur le S22+, confirmé par la capture reçue. Cette version ajoute un fond PNG statique 1280×720 bleu, sans animation ni composition graphique, et les raccourcis Steam (Ubuntu), Applications Ubuntu, Brave, Fichiers partagés et Terminal Debian. Ils sont également déclarés dans le menu des applications.

Steam et les applications Ubuntu utilisent le canal de relancement existant `steam-launch`, transmis à Debian via BL_LAUNCH_DIR. Le raccourci Steam ferme le bureau Debian et ouvre la session Steam du runtime Ubuntu existant ; Applications Ubuntu ouvre Plasma. Il ne s’agit pas d’une installation de Steam dans le système Debian. Les choix de pilote, de Proton et les installations de jeux restent ceux d’Ubuntu.

Une migration exécutée avant la session D-Bus configure xfdesktop pour les moniteurs XRandR détectés et les propriétés existantes. Elle remplace une image absente ou désactivée, préserve une image personnelle valide et utilise un marqueur pour respecter les changements ultérieurs et la suppression volontaire de raccourcis. La configuration du bureau de la .7 est actualisée sans réinstaller le système Debian.

Validation : 13 tests du helper (configuration, préservation des choix, dossier Bureau localisé, fichiers .desktop, protocole de relancement, validation des URL et graphiques PNG), plus les 4 tests du lanceur repassés. Les images totalisent moins de 100 Ko. Le mécanisme Xwayland de la .7 reste inchangé. L’affichage du nouveau fond et les raccourcis sur le S22+ nécessitent encore un essai sur l’appareil.

La migration remplace également les fonds SVG standard de Xfce par le PNG fourni : le système léger ne contient pas de chargeur SVG. Un fichier SVG présent peut donc laisser le fond noir. Ce cas est couvert par un test supplémentaire. Le helper Python a été finalisé dans l’APK compilé, puis l’APK a été réaligné et signé à nouveau ; les classes Android compilées et les bibliothèques natives sont inchangées.
