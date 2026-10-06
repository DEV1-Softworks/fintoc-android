# Contribuer

[English](../en/contributing.md) · [Español](../es/contributing.md) · [Português](../pt/contributing.md) · [Retour au README](README.md)

Merci de votre aide. Ce guide ne suppose aucune expérience préalable d'Android.

## 1. Préparer votre machine

1. Installez une version stable récente d'**Android Studio**. Il inclut le gestionnaire du SDK Android.
2. Dans *SDK Manager*, installez **Android SDK Platform 37**.
3. Vérifiez qu'un JDK 11 ou supérieur est disponible pour lancer Gradle. Le build télécharge lui-même le toolchain JDK 21 dont il a besoin.
4. Clonez le dépôt et ouvrez le dossier dans Android Studio, ou travaillez depuis le terminal avec `./gradlew`.

```bash
git clone git@github.com:DEV1-Softworks/fintoc-android.git
cd fintoc-android
./gradlew :app:assembleDebug
```

## 2. Structure du projet

```text
fintoc-android/
├── fintoc-sdk/                 # la bibliothèque publiée
│   └── src/
│       ├── main/kotlin/        # code source du SDK
│       ├── test/kotlin/        # tests unitaires (exécutés sur votre ordinateur)
│       └── androidTest/kotlin/ # tests instrumentés (exécutés sur un appareil)
├── app/                        # application d'exemple (même structure src)
├── gradle/
│   ├── libs.versions.toml      # toutes les versions de dépendances sont ici
│   ├── jacoco-coverage.gradle.kts
│   └── robolectric.gradle.kts
└── docs/                       # documentation en en, es, fr et pt
```

## 3. Exécuter les tests

| Objectif | Commande |
|---|---|
| Tests unitaires | `./gradlew testDebugUnitTest` |
| Tests instrumentés (appareil connecté) | `./gradlew connectedDebugAndroidTest` |
| Rapport de couverture | `./gradlew jacocoDebugCoverageReport` |
| Appliquer la règle des 80 % | `./gradlew jacocoDebugCoverageVerification` |
| Tout, dans le bon ordre | `./gradlew clean testDebugUnitTest connectedDebugAndroidTest jacocoDebugCoverageReport jacocoDebugCoverageVerification` |

Le rapport HTML est généré dans `<module>/build/reports/jacoco/jacocoDebugCoverageReport/html/index.html`.

**Exécutez tous les tests avant chaque commit.** Les tâches de couverture ne lancent pas les tests elles-mêmes, car les
tests instrumentés nécessitent un appareil. Si vous omettez les tâches de test, la vérification de couverture ne voit que
des données périmées ou absentes.

### Conseils pour les tests instrumentés

- Utilisez un appareil physique avec le débogage USB activé, ou un émulateur sous Android 6.0 (API 23) ou supérieur.
- **Gardez l'écran allumé et déverrouillé** pendant l'exécution des tests. Si l'écran s'éteint, les tests Compose échouent
  avec `No compose hierarchies found in the app`.
- Espresso 3.7.0 ou supérieur est nécessaire pour fonctionner sur Android 16 (déjà déclaré dans le catalogue de versions).
- Les tests de l'hôte Activity démarrent le vrai écran du SDK et font pivoter l'appareil une fois. L'écran charge la page
  publique du Widget de Fintoc, mais les tests réussissent qu'elle parvienne à se charger ou non.

## 4. Règles de code

- Style de code officiel de Kotlin, indentation par défaut de 4 espaces.
- Noms descriptifs pour les variables, fonctions et classes. Les noms d'une seule lettre ne sont acceptés que comme compteurs de boucle.
- Architecture propre : respectez les règles de couches de [Architecture](architecture.md).
- Tout dans le SDK est `internal`, sauf si cela doit être public (le mode d'API explicite l'impose).
- Compose d'abord : construisez l'interface avec Jetpack Compose. N'utilisez le XML que pour des besoins hérités, comme le manifeste.
- Toute nouvelle version de dépendance va dans `gradle/libs.versions.toml`, jamais directement dans un module.
- Tout changement de comportement s'accompagne de tests unitaires et, s'il touche à Android, de tests instrumentés. La couverture de chaque module doit rester à 80 % ou plus.

## 5. Git flow

| Branche | Rôle |
|---|---|
| `master` | Production. Aucun commit direct. |
| `develop` | Branche d'intégration. Chaque fonctionnalité y est fusionnée via une pull request. |
| `feature/<sujet>` | Une branche par fonctionnalité, créée depuis un `develop` à jour. |
| `fix/<sujet>` | Une branche par correction de bug, créée depuis un `develop` à jour. |

Il n'y a pas de branche `main` dans ce dépôt.

```mermaid
%%{init: {"gitGraph": {"mainBranchName": "master"}}}%%
gitGraph
    commit id: "initial"
    branch develop
    checkout develop
    commit id: "foundation"
    branch feature/accounts
    checkout feature/accounts
    commit id: "feat: accounts"
    checkout develop
    merge feature/accounts id: "PR merged"
    checkout master
    merge develop id: "release"
```

Les branches de fonctionnalité partent de `develop` et y reviennent via une pull request relue. `develop` rejoint `master` à chaque release.

Étapes pour une modification :

1. `git checkout develop && git pull`.
2. `git checkout -b feature/<sujet>`.
3. Faites de petits commits avec les [Conventional Commits](https://www.conventionalcommits.org/fr/) : `feat: …`, `fix: …`, `docs: …`, `test: …`, `chore: …`.
4. Exécutez tous les tests (section 3).
5. Ouvrez une pull request vers `develop` avec un résumé des changements et une référence à l'issue ou à la tâche concernée.
6. Attendez la relecture et la fusion. Ce n'est qu'ensuite que vous démarrez la fonctionnalité suivante depuis `develop`.

**Pas de pull requests empilées.** Chaque branche part de `develop`, jamais d'une autre branche de fonctionnalité.

## 6. Documentation

Tout changement qui affecte le comportement ou la configuration met à jour la documentation. Les documents se trouvent dans
`docs/` et doivent exister en quatre langues : anglais (`docs/en`), espagnol (`docs/es`), français (`docs/fr`) et portugais
(`docs/pt`). Le `README.md` à la racine est le point d'entrée.
