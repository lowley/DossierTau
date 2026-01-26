Très bonne question — et elle est **fondamentale** pour la qualité perçue d’un explorateur de fichiers. Une bottom sheet mal conçue devient vite confuse, surtout quand elle sert **à la fois** d’outil contextuel (fichier) et de panneau applicatif (favoris, infos globales).

Je vais te donner des **principes de base réellement importants**, éprouvés en design d’applications mobiles, en restant volontairement **conceptuel** (pas encore de code), pour que tu puisses ensuite les décliner proprement en Kotlin / Compose ou View.

---

## 1. Principe cardinal : _une bottom sheet = un contexte clair_

👉 **La règle n°1**  
Une bottom sheet doit toujours répondre à **une seule question mentale** pour l’utilisateur :

> _« Qu’est-ce que je peux faire ici, maintenant ? »_

Cela implique :

- **Soit** un contexte **fichier**
- **Soit** un contexte **application**
- **Jamais les deux mélangés simultanément**

### Bonne pratique

👉 **Deux modes explicites**, jamais implicites :

- **Mode “Fichier”**
    - déclenché par _long press sur un fichier_
- **Mode “Application”**
    - déclenché par un bouton dédié (menu, icône étoile, etc.)

Même si techniquement c’est **la même bottom sheet**, le **contenu et l’intention** doivent être radicalement distincts.

---

## 2. Toujours afficher un “ancrage sémantique” en haut

L’espace est restreint → le cerveau doit comprendre vite.

### En haut de la bottom sheet, toujours :

#### Pour un fichier

- **Nom du fichier**
- **Icône du type** (PDF, dossier, vidéo…)
- Optionnel : chemin abrégé ou taille

> Cela permet à l’utilisateur de ne jamais douter :  
> _« Je suis bien en train d’agir sur CE fichier »_

#### Pour l’application

- Titre clair, par ex.  
    **“Favoris”**, **“Informations”**, **“Gestion”**

📌 **Jamais de bottom sheet “sans titre”**  
C’est une erreur très fréquente.

---

## 3. Priorité absolue aux actions primaires (règle des 3–5)

Sur mobile, une bottom sheet efficace affiche :

- **3 à 5 actions maximum visibles immédiatement**

Au-delà :

- soit tu dégrades la lisibilité
- soit tu forces un scroll pénible
- soit tu dilues l’intention

### Exemple – mode fichier

**Actions primaires (haut de la liste)** :

- ⭐ Ajouter / Retirer des favoris
- 📂 Déplacer
- 🗑 Supprimer

**Actions secondaires (en bas ou sous-menu)** :

- Renommer
- Propriétés
- Partager

👉 **Les actions dangereuses** (suppression) :

- en bas
- visuellement distinctes
- jamais la première

---

## 4. Séparer clairement “actions” et “informations”

Un piège classique : mélanger infos et boutons.

### Bonne structure verticale

1. **En-tête** (contexte)
2. **Bloc Actions**
3. **Séparateur visuel**
4. **Bloc Informations** (lecture seule)

Exemple fichier :

- Actions : Favori / Déplacer / Supprimer
- Infos : taille, date, permissions

Exemple application :

- Actions : gérer favoris, trier
- Infos : nombre total, chemin racine, version app

👉 L’utilisateur **agit d’abord**, puis **consulte**.

---

## 5. Éviter l’effet “menu fourre-tout”

Très important :  
Une bottom sheet **n’est pas un menu principal déguisé**.

### Mauvaise pratique

- y mettre :
    - réglages
    - aide
    - favoris
    - actions fichier
    - informations système  
        → confusion totale

### Bonne pratique

- La bottom sheet **complète l’écran courant**
- Elle ne le remplace jamais

Si un contenu devient :

- complexe
- riche
- interactif  
    👉 il mérite **un écran dédié**.

---

## 6. Hiérarchie visuelle stricte (icône, texte, rythme)

Sur petit espace, la hiérarchie est cruciale.

### Règles simples

- Icône **avant** le texte
- Verbe d’action clair :
    - ❌ “Favori”
    - ✅ “Ajouter aux favoris”
- Espacement généreux vertical
- Zones tactiles larges (≥ 48dp)

👉 Une bottom sheet bien espacée paraît **plus rapide** même si elle ne l’est pas.

---

## 7. États visuels explicites (surtout pour les favoris)

Pour un explorateur de fichiers, c’est clé.

### Exemple “Favori”

- État ON :
    - icône étoile pleine
    - texte : “Retirer des favoris”
- État OFF :
    - étoile vide
    - “Ajouter aux favoris”

❗ Ne jamais laisser l’utilisateur deviner l’état actuel.

---

## 8. Gestuelle cohérente et prévisible

L’utilisateur **doit pouvoir fermer sans réfléchir**.

### Bonnes pratiques

- Swipe vers le bas → ferme
- Tap en dehors → ferme
- Bouton “X” inutile si les gestes sont respectés

Mais :

- **Si action critique** (ex. suppression)  
    → confirmation modale distincte

---

## 9. Une bottom sheet n’est pas un écran modal complet

Règle importante de design :

> Si l’utilisateur doit rester plus de 10 secondes dans la bottom sheet,  
> alors ce n’est plus le bon composant.

Dans ce cas :

- naviguer vers un écran
- ou ouvrir une feuille plein écran

---

## 10. Penser “évolutivité” dès maintenant

Ton idée d’une bottom sheet multi-usage est bonne **si** :

- tu as un **modèle de données clair** :
    - `BottomSheetMode = File | App`
- un **contenu totalement découplé**
- aucune logique “if spaghetti” dans l’UI

Cela t’évitera une dette UX énorme plus tard.

---

### En résumé (les règles essentielles)

Si je devais condenser :

1. **Un seul contexte à la fois**
2. **Toujours un titre clair**
3. **3–5 actions max visibles**
4. **Actions avant informations**
5. **Pas de menu fourre-tout**
6. **États visibles (favoris !)**
7. **Si c’est complexe → écran dédié**

---

Si tu veux, au prochain échange, je peux :

- te proposer **une structure de composables Compose** idéale
- ou te faire **un schéma mental** (UI state + events)
- ou analyser **un cas précis** de ton explorateur

Dis-moi ce qui t’aiderait le plus maintenant.