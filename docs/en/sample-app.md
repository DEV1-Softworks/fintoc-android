# Sample app

[Español](../es/sample-app.md) · [Français](../fr/sample-app.md) · [Português](../pt/sample-app.md) · [Back to README](../../README.md)

The `app` module is a small Compose app that uses every part of the SDK, so you can try it against Fintoc's sandbox. It
holds **no secret key** and cannot create session tokens: your backend does, as in a real app. The sample asks you to
paste what your backend would give it.

## Run it

1. Take your sandbox public key (`pk_test_…`) from the Fintoc dashboard.
2. Add it to `local.properties`, which Git ignores:

```properties
fintoc.publicKey=pk_test_your_key
```

3. Run `./gradlew :app:installDebug`, or run the `app` module from Android Studio.

The build refuses a secret key (`sk_…`) and anything that does not look like `pk_test_…` or `pk_live_…`. Without a key the
app uses a placeholder and warns about it on the home screen, and the Widget cannot start real flows.

## What each demo shows

| Demo | SDK API | What it needs |
|---|---|---|
| Connect a bank account | `FintocWidget` with `Movements` | Only your public key |
| Pay with the Widget | `FintocWidget` with `Payments` | A session token |
| Pay with a token provider | `FintocWidget` with a `suspend` provider | A session token. A simulated backend hands it over after a pause, and can fail once to show the retry. |
| Pay in a screen of its own | `FintocWidgetContract` | A session token |
| Hosted checkout | `FintocHostedCheckout` | A `redirect_url` |

Under each Widget the sample lists the events it reports. It never prints tokens: a bank connection only says that an
exchange token arrived. The language picker on the home screen sets `FintocConfiguration.language`, which changes the
SDK's own texts.

## Get a session token or a redirect URL

Create a Checkout Session from your terminal with your **secret** key. The key stays in your shell and never goes into the
app. The `amount` is in the smallest unit of the currency, so `1000` is MXN 10.00:

```bash
read -rs FINTOC_SECRET_KEY    # type or paste your sk_test_… key, then press Enter
curl --request POST "https://api.fintoc.com/v2/checkout_sessions" \
  --header "Authorization: $FINTOC_SECRET_KEY" \
  --header "Content-Type: application/json" \
  --data-raw '{
    "amount": 1000,
    "currency": "MXN",
    "success_url": "ADDRESS SHOWN BY THE APP",
    "cancel_url": "ADDRESS SHOWN BY THE APP"
  }'
```

Fintoc's guide lists `amount`, `currency`, `success_url` and `cancel_url` as the required parameters. Check its
[API reference](https://docs.fintoc.com/api/payments-api/checkout-sessions/checkout-sessions-create) for anything else
your account needs. The response has the `redirect_url` of the hosted checkout and, for the Widget, the `session_token`.
If yours says `session_token: null`, look in the API reference for the `ui_mode` that applies to Widget payments.

- For the **Widget** demos, paste the `session_token`.
- For the **hosted checkout** demo, use the two addresses that the screen shows as `success_url` and `cancel_url`, then
  paste the `redirect_url`.

Use Fintoc's [test credentials](https://docs.fintoc.com/guides/resources/test-mode) to complete a payment in the sandbox.

## What running it can settle

The SDK was built from Fintoc's documentation and tested on a device, but some behaviors can only be seen with a real
session. The sample is the place to check them:

- **Widget events.** With a real `pk_test_` key the event log should fill up, starting with `opened`. The SDK always sends
  `_on_event=true`: if events do not arrive, start there.
- **A secret value in the return address.** The hosted checkout demo puts a random value in both addresses. After paying,
  the sample should say that the success address came back. If Fintoc rejects or drops the query string, the SDK would
  need another way to carry the value.
- **A custom scheme as a return address.** The sample uses `fintocsample://`. If Fintoc refuses it, use an `https` App
  Link of your own domain.
- **Closing the Custom Tab** reports nothing to the app, by design: refresh the status from your backend.

## How it is built

- One `singleTask` Activity that handles configuration changes itself, so a Widget survives a rotation and a hosted
  checkout that comes back reaches the Activity that is already open.
- `SampleSdk` is the only place that configures the SDK, and the language picker calls it again.
- Texts in English, Spanish, French and Portuguese.
- The release build uses R8, so every build checks that the SDK works minified.
- Tests: Robolectric for the logic and the screens, and a real device for the return links, the SDK's own screen and
  accessibility (Google's Accessibility Test Framework).
