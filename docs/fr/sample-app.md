# Application d'exemple

[English](../en/sample-app.md) · [Español](../es/sample-app.md) · [Português](../pt/sample-app.md) · [Retour au README](../../README.md)

Le module `app` est une petite application Compose qui utilise chaque partie du SDK, pour que vous l'essayiez contre le
sandbox de Fintoc. Elle **ne détient aucune clé secrète** et ne peut pas créer de session tokens : c'est votre backend qui
le fait, comme dans une vraie app. L'exemple vous demande de coller ce que votre backend lui donnerait.

## Lancez-la

1. Prenez votre clé publique sandbox (`pk_test_…`) dans le dashboard de Fintoc.
2. Ajoutez-la à `local.properties`, que Git ignore :

```properties
fintoc.publicKey=pk_test_your_key
```

3. Lancez `./gradlew :app:installDebug`, ou exécutez le module `app` depuis Android Studio.

Le build refuse une clé secrète (`sk_…`) et tout ce qui ne ressemble pas à `pk_test_…` ou `pk_live_…`. Sans clé, l'app
utilise une clé factice et l'indique sur l'écran d'accueil, et le Widget ne peut pas lancer de vrais flux.

## Ce que montre chaque démo

| Démo | API du SDK | Ce qu'elle demande |
|---|---|---|
| Connecter un compte bancaire | `FintocWidget` avec `Movements` | Seulement votre clé publique |
| Payer avec le Widget | `FintocWidget` avec `Payments` | Un session token |
| Payer avec un fournisseur de jeton | `FintocWidget` avec un fournisseur `suspend` | Un session token. Un backend simulé le remet après une pause, et peut échouer une fois pour montrer la nouvelle tentative. |
| Payer dans un écran dédié | `FintocWidgetContract` | Un session token |
| Checkout hébergé | `FintocHostedCheckout` | Un `redirect_url` |

Sous chaque Widget, l'exemple liste les événements qu'il signale. Il n'affiche jamais de jetons : une connexion bancaire
dit seulement qu'un exchange token est arrivé. Le sélecteur de langue de l'écran d'accueil définit
`FintocConfiguration.language`, qui change les textes propres au SDK.

## Obtenez un session token ou une URL de redirection

Créez une Checkout Session depuis votre terminal avec votre clé **secrète**. La clé reste dans votre shell et n'entre
jamais dans l'app. Le `amount` est exprimé dans la plus petite unité de la devise : `1000` vaut donc 10,00 MXN :

```bash
read -rs FINTOC_SECRET_KEY    # tapez ou collez votre clé sk_test_…, puis Entrée
curl --request POST "https://api.fintoc.com/v2/checkout_sessions" \
  --header "Authorization: $FINTOC_SECRET_KEY" \
  --header "Content-Type: application/json" \
  --data-raw '{
    "amount": 1000,
    "currency": "MXN",
    "success_url": "ADRESSE AFFICHÉE PAR L'APP",
    "cancel_url": "ADRESSE AFFICHÉE PAR L'APP"
  }'
```

Le guide de Fintoc liste `amount`, `currency`, `success_url` et `cancel_url` comme paramètres obligatoires. Consultez sa
[référence d'API](https://docs.fintoc.com/api/payments-api/checkout-sessions/checkout-sessions-create) pour tout autre
paramètre dont votre compte a besoin. La réponse contient le `redirect_url` du checkout hébergé et, pour le Widget, le
`session_token`. Si le vôtre indique `session_token: null`, cherchez dans la référence le `ui_mode` qui s'applique aux
paiements par Widget.

- Pour les démos du **Widget**, collez le `session_token`.
- Pour la démo du **checkout hébergé**, utilisez les deux adresses affichées par l'écran comme `success_url` et
  `cancel_url`, puis collez le `redirect_url`.

Utilisez les [identifiants de test](https://docs.fintoc.com/guides/resources/test-mode) de Fintoc pour terminer un
paiement dans le sandbox.

## Ce qui a été vérifié contre le sandbox

Le SDK a été construit à partir de la documentation de Fintoc et testé sur un appareil. Exécuter cet exemple contre le
sandbox de Fintoc a confirmé :

- **Événements du Widget.** Avec une vraie clé `pk_test_`, le journal d'événements se remplit, à commencer par `opened`.
  Le SDK envoie toujours `_on_event=true`.
- **Une valeur secrète dans l'adresse de retour.** La démo du checkout hébergé met une valeur aléatoire dans les deux
  adresses, et Fintoc renvoie la query string : l'exemple distingue donc l'adresse de succès qui porte votre valeur
  d'une adresse falsifiée.
- **Un schéma personnalisé comme adresse de retour.** Fintoc accepte `fintocsample://`. Une vraie app devrait malgré
  tout préférer un App Link `https` de son propre domaine, qu'aucune autre app ne peut revendiquer.

Reste ouvert :

- **Quand une Checkout Session a un `session_token`.** La documentation de Fintoc montre un `redirect_url` et un
  `session_token`, mais pas quand le jeton vaut `null`. Consultez la réponse de votre session et la référence d'API de
  Fintoc pour le `ui_mode` qui s'applique aux paiements par Widget.

Fermer la Custom Tab ne signale rien à l'app, par conception : actualisez l'état depuis votre backend.

## Comment elle est construite

- Une seule Activity `singleTask` qui gère elle-même les changements de configuration : un Widget survit donc à une
  rotation, et un checkout hébergé qui revient atteint l'Activity déjà ouverte.
- `SampleSdk` est le seul endroit qui configure le SDK, et le sélecteur de langue l'appelle de nouveau.
- Textes en français, anglais, espagnol et portugais.
- Le build release utilise R8 : chaque build vérifie donc que le SDK fonctionne minifié.
- Tests : Robolectric pour la logique et les écrans, et un appareil réel pour les liens de retour, l'écran propre au SDK
  et l'accessibilité (l'Accessibility Test Framework de Google).
