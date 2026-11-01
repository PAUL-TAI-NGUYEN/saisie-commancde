# Commandes - application Android

Application Android native de saisie vocale de commandes. Même écran que la version PWA (client, date, article.xml, code article, quantité, prix, export CSV), mais :

- **Rien à héberger** : l'écran est intégré dans l'application, elle marche sans internet (sauf la reconnaissance vocale si le téléphone n'a pas le français hors ligne).
- **Micro autorisé une seule fois**, au premier lancement. Utilise le moteur vocal d'Android (celui de Google), en français.
- **article.xml mémorisé** : il suffit de le choisir une fois ; l'application le garde. On le rechoisit seulement pour le mettre à jour.
- **Export CSV** : le fichier est enregistré dans *Téléchargements* (nom `commandes_AAAA-MM-JJ_HHhMM.csv`), puis Android propose de l'envoyer (mail, WhatsApp, Drive...).
- **Bouton retour** du téléphone : revient de la page 2 à la page 1.
- Android 8.0 ou plus récent.

## Obtenir le fichier APK

Choisissez une des deux méthodes.

### Méthode A : avec GitHub (rien à installer sur l'ordinateur)

1. Créez un compte gratuit sur github.com, puis un nouveau dépôt (bouton **New**), par exemple `commandes-android`.
2. Dans le dépôt, cliquez sur **uploading an existing file** et glissez **tout le contenu** de ce dossier (y compris le dossier `.github`). Validez avec **Commit changes**.
   - Si le dossier `.github` n'est pas pris (il est caché sous Windows), créez à la main le fichier `.github/workflows/construire-apk.yml` via **Add file > Create new file** et collez son contenu.
3. Ouvrez l'onglet **Actions** : la compilation démarre seule (environ 3 à 5 minutes).
4. Quand elle est verte, cliquez dessus, puis en bas sur **Commandes-apk** pour télécharger un zip contenant `app-debug.apk`.

### Méthode B : avec Android Studio

1. Installez Android Studio (developer.android.com/studio).
2. **File > Open** et choisissez ce dossier. Attendez la fin de la synchronisation (première fois : plusieurs minutes).
3. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. Cliquez sur **locate** dans la notification : le fichier est `app/build/outputs/apk/debug/app-debug.apk`.

## Installer sur le téléphone

1. Copiez `app-debug.apk` sur le téléphone (câble USB, mail, Drive...).
2. Ouvrez-le depuis l'application *Fichiers*. Android demande d'autoriser l'installation d'applications inconnues pour cette source : acceptez.
3. Lancez **Commandes**, autorisez le micro, choisissez votre `article.xml`.

## Modifier l'écran

L'écran est le fichier `app/src/main/assets/index.html`. Après une modification, recompilez (méthode A : renvoyez le fichier sur GitHub ; méthode B : Build APK).

## Contenu du dossier

- `app/src/main/assets/index.html` : l'écran (même logique que la PWA, branchée sur Android)
- `app/src/main/java/fr/ngplus/commandes/MainActivity.java` : micro, choix du fichier, enregistrement du CSV, mémorisation des articles
- `app/src/main/AndroidManifest.xml` : nom, icône, autorisation micro
- `app/src/main/res/` : icône (micro blanc sur fond bleu) et couleurs
- `.github/workflows/construire-apk.yml` : compilation automatique sur GitHub
- `build.gradle`, `settings.gradle`, `gradlew` : fichiers de compilation
