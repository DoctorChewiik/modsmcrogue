# Rogue Tower (mod Fabric, Minecraft 1.21.1)

Transforme Minecraft en roguelite : vue du dessus, classes avec pouvoirs, et une tour infinie
remplie de monstres et d'animaux agressifs.

## Installation

1. Installer **Fabric Loader** pour Minecraft **1.21.1** (https://fabricmc.net/use/installer/).
2. Mettre **Fabric API** (version 1.21.1) dans le dossier `mods`.
3. Glisser `roguetower-1.0.0.jar` dans le dossier `mods`.

Le .jar se télécharge dans l'onglet **Releases** du dépôt (compilé automatiquement par GitHub Actions).

## Jouer

- **Portail** : construire un cadre en **cobblestone** dont l'ouverture fait **4x4** (comme un portail du Nether,
  les coins sont facultatifs), puis l'allumer au **briquet**.
- **Classe** : à l'entrée de la tour (ou touche **J**), choisir Guerrier, Archer, Mage, Assassin ou Nécromancien.
- **R** : compétence de classe. **F** : sort (remplaçable). **H** : vue du dessus auto / toujours / désactivée.
  Si « Échanger les mains » était sur F, il est déplacé sur G.
- **Étages** : tuer tous les ennemis, ouvrir le coffre de récompense, puis marcher sur la plateforme d'obsidienne
  pleureuse pour monter. S'accroupir sur la plateforme pour quitter la tour. Un gardien (boss) tous les 5 étages.
- **Butin** : Commun, Peu commun, Rare, Épique (armes magiques), Légendaire (armes uniques). Les chances augmentent
  avec l'étage (`/roguetower chances`). Les **livres de sort** remplacent le sort de la touche F (clic droit).
- Mourir dans la tour termine la partie.

## Commandes

- `/roguetower classe` : ouvrir le choix de classe
- `/roguetower chances` : chances de butin à l'étage actuel
- `/roguetower quitter` : quitter la tour
- `/roguetower etage <n>` (op) : aller à un étage
- `/roguetower livre <SORT>` (op) : obtenir un livre de sort (ex. `METEOR`)
- `/roguetower butin <rarete>` (op) : obtenir un objet (ex. `legendary`)
