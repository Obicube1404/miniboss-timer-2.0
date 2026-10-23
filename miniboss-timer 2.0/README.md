# MiniBoss Timer (Minecraft 26.2 / Fabric, client uniquement)

Quand un mini-boss meurt près de toi, le mod affiche dans le chat :

    [MiniBoss] Capitaine tue sur East Blue - respawn dans 15:00 (a 21:34:12)

et garde un panneau en haut à gauche avec tous les timers en cours (serveur + boss + temps restant).
Une alerte (chat + son) est envoyée 30 s avant le respawn, puis quand le boss est de retour.
Les timers sont sauvegardés : ils survivent à un redémarrage du jeu.

Le mod est 100 % côté client : rien à installer sur le serveur, donc compatible Minepiece.

## Obtenir le .jar sans rien installer (GitHub)

1. Cree un compte sur github.com et un nouveau depot (repository) vide.
2. Envoie-y tout le contenu de ce dossier (bouton "Add file" > "Upload files"), y compris le dossier `.github`.
3. Onglet **Actions** > "Build mod jar" > le build se lance tout seul (ou "Run workflow").
4. Quand il est vert, ouvre-le et telecharge l'artifact `miniboss-timer-jar` (un zip contenant le .jar).
   Le jar du mod est celui qui ne finit pas par `-sources.jar`.

## Compiler en local

Prérequis : JDK 25.

Option A (recommandée, la plus sûre) :
1. Va sur https://fabricmc.net/develop/template/ , choisis Minecraft 26.2, génère le projet.
2. Remplace son dossier `src/` par celui de ce projet, et garde son build.gradle / gradle.properties.
3. `./gradlew build` -> le jar est dans `build/libs/`.

Option B : dans ce dossier, installe Gradle 9.5+ puis lance `gradle wrapper` une fois, puis `./gradlew build`.

## Installer

Mets le jar de `build/libs/` (celui sans `-sources`) dans `.minecraft/mods/`, avec
Fabric Loader 0.19+ et Fabric API pour 26.2.

## Utilisation

Au premier lancement, le mod détecte comme "mini-boss" toute entité avec un nom personnalisé
et au moins 100 PV. Pour être précis :

1. `/mbt debug` puis tue un mini-boss : le chat affiche le nom exact de l'entité morte.
2. `/mbt add <nom>` pour l'ajouter à la liste (puis `/mbt debug` pour couper les messages).

Commandes (`/mbt` ne passe jamais par le serveur) :

| Commande | Effet |
|---|---|
| `/mbt list` | timers en cours |
| `/mbt start <boss>` | lancer un timer à la main |
| `/mbt add <boss>` / `remove <boss>` / `bosses` | gérer la liste |
| `/mbt minutes <n>` | respawn par défaut (15) |
| `/mbt server [nom\|auto]` | voir / forcer le nom du serveur |
| `/mbt hud` | afficher / masquer le panneau |
| `/mbt clear` | supprimer tous les timers |
| `/mbt reload` | recharger la config |

## Nom du serveur et /voyager

Le mod lit le menu NAVIGATION du `/voyager` : quand tu cliques sur une icône dont le texte contient
"spawn" (option `navItemRegex`) puis que tu changes réellement de serveur, le nom de l'icône devient
le serveur actuel (message "Serveur : ..." dans le chat). Chaque serveur a ses propres timers.

Pour voir ce que le mod lit : `/mbt debug`, ouvre `/voyager`, survole les icônes -> le chat affiche
leur texte exact. Si le nom du serveur est dans une autre ligne que la première, ou contient un
nombre de joueurs qui change, règle `navServerNameRegex` (groupe 1 = le nom à garder).

Sans détection : nom du serveur dans ta liste multijoueur. Tu peux aussi forcer avec
`/mbt server <nom>` (et `/mbt server auto` pour revenir en automatique).

## Config : `.minecraft/config/miniboss_timer.json`

- `bosses` : `{"Capitaine": 15, "Amiral": 20}` -> temps de respawn propre à chaque boss (0 = défaut)
- `useHealthHeuristic` / `minBossMaxHealth` : détection automatique par PV
- `navItemRegex` / `navServerNameRegex` : reconnaissance des icônes serveur du /voyager
- `nameCleanupRegex` : retire des morceaux de nom (ex. niveau entre crochets)
- `chatKillPatterns` : regex de messages de chat annonçant une mort de boss (groupe 1 = nom du boss)
- `hudX`, `hudY`, `preAlertSeconds`, `playSound`, `keepAfterReadySeconds`, `maxKillDistance`

Le timer démarre que ce soit toi ou un autre joueur qui ait tué le boss : le respawn est le même.
