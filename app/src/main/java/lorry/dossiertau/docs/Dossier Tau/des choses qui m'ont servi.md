><font color="#c00000">remplace toutes les occurences de '-' par ',' dans les sous-dossiers de niveau 1 de filles:</font>
```
adb shell "for dir in /storage/emulated/0/Movies/sexe/filles/*/; do [ -d \"\$dir\" ] && mv \"\$dir\" \"\${dir//-/,}\"; done"
```
><font color="#c00000">Merci maintenant, je voudrais une autre commande qui fasse quelque chose d'un peu plus compliqué cette fois-ci toujours sur des sous dossiers d'un dossier donné à au niveau 1. Il s'agirait. De alors, il y a deux cas possibles, soit le sous dossier contient un seul nom soit il contient plusieurs noms séparés par des virgules. Et pour chaque portion s'il y en a plus d'une je voudrais que la deuxième devienne la première et que la première devienne la deuxième. Et les autres sont conservés tel quel. Je voudrais que tu me donnes la commande et que tu me donnes un exemple pour que je vois si tu as bien compris ce qu'il faut faire et ta manière de le faire.</font>

```
adb shell "for dir in /storage/emulated/0/Movies/sexe/filles/*/; do [ -d \"\$dir\" ] || continue; base=\$(basename \"\$dir\"); if [[ \"\$base\" == *\",\"* ]]; then first=\$(echo \"\$base\" | cut -d',' -f1); second=\$(echo \"\$base\" | cut -d',' -f2); rest=\$(echo \"\$base\" | cut -d',' -f3-); if [ -n \"\$rest\" ]; then newname=\"\$second,\$first,\$rest\"; else newname=\"\$second,\$first\"; fi; mv \"\$dir\" \"\$(dirname \"\$dir\")/\$newname\"; fi; done"
```

><font color="#c00000">preview</font>

```
adb shell "for dir in /storage/emulated/0/Movies/sexe/filles/*/; do [ -d \"\$dir\" ] || continue; base=\$(basename \"\$dir\"); if [[ \"\$base\" == *\",\"* ]]; then first=\$(echo \"\$base\" | cut -d',' -f1); second=\$(echo \"\$base\" | cut -d',' -f2); rest=\$(echo \"\$base\" | cut -d',' -f3-); if [ -n \"\$rest\" ]; then newname=\"\$second,\$first,\$rest\"; else newname=\"\$second,\$first\"; fi; echo \"[ACTION] \$base  ==>  \$newname\"; else echo \"[IGNORÉ] \$base (pas de virgule)\"; fi; done"
```


