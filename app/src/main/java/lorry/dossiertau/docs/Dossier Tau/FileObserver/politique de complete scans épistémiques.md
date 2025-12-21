>Voilà, ma question c'est que je suis en train de refaire les tests, ils passent quasiment tous, mais j'ai un problème pour l'un d'eux, et en fait le truc c'est que c'est à propos du global scan qu'il y a au début, je me demande à quoi ça sert de faire un global scan finalement, parce que avant le global scan était intéressant parce qu'il s'opposait à des diffs atomiques causés par des events qui étaient eux aussi atomiques, mais maintenant que quand je reçois une information de changement, je fais des diffs et je les envoie, disons qu'au départ je pensais remplacer le global scan par une sorte de diff comparé à un ensemble vide, c'est à dire que ça aurait été un diff avec que des create, mais par contre ce que je me dis maintenant c'est qu'il faut peut-être quand même garder les global scans parce qu'au bout par exemple d'un certain nombre de diffs donnés, on peut rescanner par défaut, et donc il faudrait quand même avoir des scans globaux, mais ce ne serait plus, enfin disons ce serait pour comparer, pour s'assurer que la base est toujours juste.

Oui — **garder un “global scan” a encore du sens**, même si tu as désormais des diffs “propres” envoyés en batch. La raison n’est plus “philosophique” (global vs atomique), mais **épistémique** : _comment être sûr que ta base représente bien la réalité du disque_.

Voilà les rôles utiles d’un global scan dans TAU, et comment l’intégrer sans casser tes tests.

---

## 1) À quoi sert un global scan, maintenant ?

### A) Résilience aux pertes d’événements

Même avec un pipeline impeccable, il existe des cas où tu peux rater des changements :

- FileObserver/Watchers qui dropent (buffers OS, limitations)
- pics d’activité (renames en rafale, décompression d’archives, synchro cloud)
- arrêt/restart du service, reboot, crash
- permissions / chemins temporairement indisponibles
- événements “coalescés” par le système (tu n’as pas toujours le détail exact)

👉 Le global scan sert de **réconciliation** : “quelle est la vérité _maintenant_ ?”.

### B) Correction des dérives de modèle

Même si tu ne perds pas d’événements, ton interprétation peut être imparfaite :

- bugs de mapping (create vs move)
- conflits (suite d’events ambigus)
- incohérences DB (transactions interrompues, migration, etc.)

👉 Le scan sert de **contrôle qualité** et de **réparation**.

### C) Initialisation correcte au démarrage

Au lancement, tu as besoin de connaître :

- l’image initiale du dossier (sinon tu ne peux pas interpréter certains diffs)
- ou, à défaut, de “poser une base” fiable.

Ton idée “diff vs ensemble vide = all create” est viable… **mais** elle suppose que tu peux scanner le disque pour produire ces “create” — donc tu reviens à un scan, juste renommé.

---

## 2) Ton intuition est bonne : global scan ≠ vieux modèle

Avant : global scan = “gros événement” opposé aux events atomiques.  
Maintenant : global scan = **snapshot de référence** (baseline) + éventuellement “réparation”.

Tu peux le formaliser comme :

- `ScanBatch` (snapshot complet ou pseudo-diff)
- `EventBatch` (diffs issus des TIC/POC)

Les deux deviennent des **batches** qui alimentent le même pipeline, mais avec une **intention** différente.

---

## 3) Stratégie propre : “scan comme garde-fou”, pas comme routine lourde

### Option recommandée : “scan ponctuel et motivé”

Déclencher un scan quand :

-<span style="background:#ff4d4f"> le service redémarre et la base n’est pas “validée”</span>
<span style="background:#ff4d4f">- tu détectes une anomalie (ex. diff impossible à appliquer, missing parent, etc.)</span>
<span style="background:#ff4d4f">- toutes les N minutes/heures (faible fréquence)</span>
<span style="background:#ff4d4f">- après X diffs (comme tu l’as dit), si tu veux un garde-fou simple</span>

Le plus important : **ne pas scanner trop souvent**, mais scanner assez pour être robuste.

---

## 4) Comment concilier “scan” et “diffs TIC/POC” conceptuellement

Tu peux garder ton idée, mais en la rendant explicite :

### A) “Scan → Diff vs référence DB”

Le scan produit un snapshot `S_disk`.  
Tu compares à `S_db` (l’image courante en base).  
Tu produis un batch de diffs “Spy Levels” :

- creates, deletes, moves/renames, modifies  
    Et tu émets **un seul batch**.
    

👉 Là, tu n’as pas un scan “à part”, tu as un **reconcile batch**.

### B) “Scan initial → baseline”

Au tout premier démarrage (ou DB vide) :

- soit tu écris directement l’image (table “items courants”)
    
- soit tu émets un batch “all create”  
    Dans les deux cas, c’est un scan, mais avec une intention “bootstrap”.
    

---

## 5) Impact sur tes tests (là où ça coince chez toi)

Le piège habituel : un scan “au début” déclenche des émissions que le test ne prend pas en compte, ou bien il introduit une asynchronie.

Deux solutions de design test-friendly :

### ✅ 1) Le scan initial est **optionnel / injectable**

Ex. une dépendance `StartupPolicy` ou `ScanPolicy` :

- en prod : scan au démarrage selon règles
    
- en tests : désactivé, ou déclenché explicitement par le test
    

→ tes tests deviennent déterministes.

### ✅ 2) Le scan initial émet un **événement typé**

Ex. `BatchSource = STARTUP_SCAN | TIC_EVENT | PERIODIC_RECONCILE`  
Ainsi, dans les tests :

- tu peux filtrer `STARTUP_SCAN`
    
- ou au contraire l’asserter explicitement
    

---

## 6) Ce que je te conseille (décision)

- **Oui**, garde le global scan, mais renomme-le mentalement en **reconcile / snapshot baseline**.
    
- Fais-le :
    
    - au démarrage (si nécessaire)
        
    - périodiquement ou après X diffs (garde-fou)
        
    - ou quand tu détectes un problème
        
- Et surtout : **rends son déclenchement testable** (désactivable/injectable), sinon tu auras toujours “un test qui casse”.
    

---

>Je suis en train de me demander si on ne pourrait pas faire un truc pour garder en base une image symbolique d'un répertoire. Imaginons qu'on a qu'un répertoire qui est suivi, pour simplifier. En fait, quand on envoie des diffs dans la base, on pourrait très bien imaginer qu'en parallèle, on écrit et je ne sais plus si on l'avait déjà évoqué l'idée, mais on écrit dans un autre endroit des lignes qui symbolisent ce qu'il y a dedans, et non plus les diffs. Par exemple, si je crée un fichier TOTO dans un répertoire, alors la table qui correspond à ce répertoire, elle contiendra une ligne qui sera de type fichier et qui aura le nom TOTO. Et si je renomme plus tard dans mon répertoire le fichier en TITI, alors en parallèle de l'écriture sur la base, ça ne devrait pas coûter beaucoup, je regarde la ligne où est écrit TOTO et je renomme le TOTO en TITI. Comme ça, avec ça, on a toujours une image en base du contenu du répertoire. Cela permet d'une part de détecter les erreurs avant de les envoyer à FolderTO et d'autre part, lorsque l'on fait des réconciliations, ça accélère énormément le calcul de ce qu'il y a dans le répertoire. Qu'en penses-tu ?

Oui, c’est une très bonne direction — et, pour TAU, je dirais même : **c’est la forme “adulte”** du système.

Tu décris exactement le couple classique :

1. **Journal des événements (diffs)** = _ce qui est arrivé_
    
2. **Vue matérialisée (image symbolique)** = _à quoi ressemble le dossier maintenant_
    

Et l’idée clé : **tu mets à jour la vue au fil de l’eau** quand tu insères les diffs, au lieu de la reconstruire à chaque fois.

---

## Pourquoi c’est pertinent (et ce que ça t’apporte vraiment)

### 1) Vérification immédiate des incohérences

Si tu appliques un diff à une image en base, tu peux détecter :

- delete d’un fichier absent
    
- rename d’un fichier absent
    
- create déjà présent
    
- move impossible (parent absent, collision de nom, etc.)
    

👉 Donc tu peux **refuser**, **corriger**, ou **marquer comme suspect** avant d’alimenter FolderTO / UI.

### 2) Réconciliations beaucoup plus rapides

Aujourd’hui, “réconcilier” = souvent :

- rescanner disque
    
- recomposer une image
    
- comparer
    

Avec une image en base :

- tu compares _snapshot disque_ vs _image DB_
    
- et tu produis un batch de diffs de correction
    

👉 Tu supprimes une grosse partie du coût : **tu as déjà une référence**.

### 3) Temps réel plus propre

Tu veux afficher un dossier ? Tu lis directement la table “image”.  
Tu veux l’historique ? Tu lis la table “diffs”.

👉 Tu sépares parfaitement “lecture état courant” et “audit”.

---

## Le point crucial : faire ça sans te tirer une balle dans le pied

### 1) Toujours faire “diff + mise à jour image” dans **la même transaction**

Sinon tu risques :

- diff écrit mais image pas mise à jour
    
- ou l’inverse
    

Donc : une intention CIA → **transaction unique** :

- insert diffs (journal)
    
- apply diffs sur l’image (vue matérialisée)
    
- éventuellement update “metadata dossier” (version, lastScanId, etc.)
    

### 2) L’image doit être indexée intelligemment

Ta phrase « je regarde la ligne TOTO et je renomme » : oui, mais il te faut une clé stable.

Deux options :

**Option A (simple) : clé = full_path**

- table image : `full_path` unique
    
- rename/move = update de `full_path`
    
- ⚠️ mais un rename implique de mettre à jour aussi les enfants si c’est un dossier (cascade)
    

**Option B (robuste) : clé = itemId stable**

- tu attribues un `id` à l’item (logique DB)
    
- tu gardes `full_path` comme donnée, indexée
    
- rename = update du champ `name/full_path` sur l’itemId
    
- move = update du parent + recompute full_path
    
- ✅ plus propre pour l’évolution, mais demande un peu plus d’infrastructure
    

Pour un _seul répertoire suivi_ au début, Option A suffit souvent.

### 3) Le cas “rename/move ambigu”

Dans la vraie vie, le watcher peut donner :

- un DELETE + CREATE au lieu d’un RENAME
    
- ou des événements incomplets
    

L’image en base permet justement d’inférer :

- si delete(TOTO) et create(TITI) arrivent quasi ensemble, tu peux (peut-être) les regrouper en rename
    
- ou accepter que ce soit deux opérations
    

👉 Ce n’est pas un défaut : c’est une capacité.

---

## Comment ça s’articule avec tes “Spy Levels / CIA Levels”

Je verrais bien :

- **Spy Levels** : propose un batch de changements (peut être imparfait)
    
- **CIA** : “normalise” → produit une _Intention_ :
    
    - `ApplyBatchToDb(batch, source, timestamp, maybeScanId)`
        
- **DB layer** :
    
    - écrit le batch dans la table `diffs`
        
    - applique le batch sur la table `folder_image`
        

Et là tu as ton bénéfice majeur :

> CIA peut décider de “rejeter” un diff incohérent _avant_ d’impacter l’état courant.

---

## Ce que je ferais à ta place (choix pragmatique)

Pour rester simple et efficace dès maintenant :

### Tables

1. `file_diffs` : journal (comme tu as)
    
2. `folder_items` : image symbolique du dossier (état courant)
    

### Règle d’or

**Toute insertion de diffs passe par une fonction unique** (une “porte”) qui :

- démarre une transaction
    
- insère le batch dans `file_diffs`
    
- applique le batch sur `folder_items`
    
- commit
    

### Bonus (facultatif mais très utile)

Une table ou colonne “health” :

- `last_applied_batch_id`
    
- `inconsistency_count`
    
- `needs_reconcile`
    

---

## Petite remarque sur ton texte (grammaire)

- « _Imaginons qu’on a qu’un répertoire_ » → plutôt : **« Imaginons qu’on n’ait qu’un seul répertoire »**.
    
- « _quand on envoie des diffs dans la base_ » → ok, mais “différences” est plus français si tu veux.
    

---

Si tu veux, on peut faire l’étape suivante très concrète :  
**définir 6 opérations minimales** que ton “apply batch” doit supporter sur `folder_items` (create/delete/rename/move/modify + “upsert”), et les invariants associés.

Tu préfères qu’on commence par :

1. le **schéma minimal** (champs exacts de `folder_items`)  
    ou
    
2. les **règles d’application** d’un batch (avec les cas tordus : collisions, delete absent, rename ambigu) ?

>En fait, je crois que tu as complété mon idée par autre chose dans ta réponse, et je voudrais en être sûr. C'est qu'en fait, on ne garde pas l'ancien snapshot dans Spy. Au lieu de cela, on lit le nouveau snapshot. Je veux dire, dans le cas où l'on reçoit par exemple un tic, on lit le contenu théorique du répertoire dans la base, et on compare les deux.

Oui — tu as bien repéré : **ce que tu proposes ici est légèrement différent (et, à mon sens, meilleur) que “Spy garde l’ancien snapshot en mémoire”.**

Tu décris un modèle où **la base devient la mémoire de référence**, et où, à chaque TIC, `Spy` (ou le composant chargé du calcul) fait :

1. lire **le snapshot réel** du disque (ou “nouveau snapshot”)
    
2. lire **l’image symbolique** en base (snapshot théorique)
    
3. comparer (DB vs disque)
    
4. émettre un **batch de diffs** (Spy Levels)
    
5. puis la base applique ces diffs et met à jour l’image
    

👉 Donc oui : **au lieu de comparer “ancien snapshot en RAM” vs “nouveau snapshot disque”**, tu compares **“snapshot DB” vs “snapshot disque”**.

---

## Ce que ça change exactement

### Avant (mémoire dans Spy)

- Spy garde `S_old` (snapshot précédent)
    
- TIC arrive → Spy lit `S_new` sur disque
    
- compare `S_old` vs `S_new`
    
- puis met à jour `S_old = S_new`
    

✅ rapide (pas besoin DB pour calculer)  
❌ fragile si Spy redémarre (perte de mémoire)  
❌ plus dur à expliquer/tester (état caché)

### Maintenant (mémoire dans DB)

- DB garde `S_db` (image symbolique)
    
- TIC arrive → Spy lit `S_disk` (réel)
    
- compare `S_db` vs `S_disk`
    
- émet un batch
    
- transaction : journal + apply sur image ⇒ `S_db` devient “à jour”
    

✅ robuste aux redémarrages  
✅ testable (état visible)  
✅ naturellement “auto-correcteur” (si tu as raté des events, le prochain TIC recale)  
❌ coût : nécessite lecture DB + scan disque à chaque TIC (mais tu peux l’amortir)

---

## Attention au point subtil (mais important)

Pour que ce modèle tienne bien, il faut décider **qui a le droit de comparer** :

- **Option 1 : Spy compare (lit DB)**
    
    - Spy dépend du DAO (ou d’un repo)
        
    - simple conceptuellement, mais ça mélange “observation” et “état”
        
- **Option 2 : CIA compare (ou un composant “Reconciler”)**
    
    - Spy ne fait que signaler “TIC sur PATH”
        
    - CIA/Reconcilier lit DB + disque, calcule le batch, écrit
        
    - en architecture, c’est souvent plus “propre”
        

Les deux marchent. TAU, tel que tu l’as imaginé, penche souvent vers :

> Spy = capteur / temporisation  
> CIA = compréhension / décision

Donc je mettrais plutôt la comparaison DB↔disque côté CIA (ou un sous-module).

---

## Et sur ta question “on ne garde pas l’ancien snapshot dans Spy ?”

Dans ce modèle : **non, pas besoin.**  
Et c’est même un avantage : tu supprimes une source d’état implicite.

Tu peux éventuellement garder _un tout petit état_ dans Spy :

- anti-rafale (debounce / délai calme)
    
- “isRunning”
    
- dernier tic timestamp
    

Mais **pas l’ancien snapshot**.

---

## Mini-check pour être sûr qu’on parle de la même chose

Ce que tu proposes revient à dire :

- un **TIC** déclenche un **reconcile**,
    
- et le reconcile compare :
    
    - _image DB_ (théorique)
        
    - vs _scan disque_ (réel)
        

✅ C’est bien ça.

---

Si tu veux, on peut ensuite répondre à la question pratique qui en découle forcément :

> “Est-ce qu’on doit rescanner le dossier entier à chaque TIC, ou peut-on faire partiel ?”  
> Je peux te proposer une stratégie “progressive” (cheap au début, scan complet seulement si suspicion).