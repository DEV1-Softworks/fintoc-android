# App de ejemplo

[English](../en/sample-app.md) · [Français](../fr/sample-app.md) · [Português](../pt/sample-app.md) · [Volver al README](../../README.md)

El módulo `app` es una pequeña app de Compose que usa cada parte del SDK, para que lo pruebes contra el sandbox de
Fintoc. **No guarda ninguna llave secreta** y no puede crear session tokens: eso lo hace tu backend, como en una app
real. El ejemplo te pide que pegues lo que tu backend le daría.

## Ejecútala

1. Toma tu llave pública de sandbox (`pk_test_…`) del dashboard de Fintoc.
2. Agrégala a `local.properties`, que Git ignora:

```properties
fintoc.publicKey=pk_test_your_key
```

3. Ejecuta `./gradlew :app:installDebug`, o corre el módulo `app` desde Android Studio.

La compilación rechaza una llave secreta (`sk_…`) y todo lo que no parezca `pk_test_…` o `pk_live_…`. Sin llave, la app usa
una de relleno y lo advierte en la pantalla de inicio, y el Widget no puede iniciar flujos reales.

## Qué muestra cada demo

| Demo | API del SDK | Qué necesita |
|---|---|---|
| Conectar una cuenta bancaria | `FintocWidget` con `Movements` | Solo tu llave pública |
| Pagar con el Widget | `FintocWidget` con `Payments` | Un session token |
| Pagar con un proveedor de token | `FintocWidget` con un proveedor `suspend` | Un session token. Un backend simulado lo entrega tras una pausa, y puede fallar una vez para mostrar el reintento. |
| Pagar en una pantalla propia | `FintocWidgetContract` | Un session token |
| Checkout alojado | `FintocHostedCheckout` | Un `redirect_url` |

Debajo de cada Widget, el ejemplo lista los eventos que reporta. Nunca imprime tokens: una conexión bancaria solo dice que
llegó un exchange token. El selector de idioma de la pantalla de inicio fija `FintocConfiguration.language`, que cambia
los textos propios del SDK.

## Obtén un session token o una URL de redirección

Crea una Checkout Session desde tu terminal con tu llave **secreta**. La llave se queda en tu shell y nunca entra en la
app. El `amount` va en la unidad más pequeña de la moneda, así que `1000` son MXN 10.00:

```bash
read -rs FINTOC_SECRET_KEY    # escribe o pega tu llave sk_test_… y pulsa Enter
curl --request POST "https://api.fintoc.com/v2/checkout_sessions" \
  --header "Authorization: $FINTOC_SECRET_KEY" \
  --header "Content-Type: application/json" \
  --data-raw '{
    "amount": 1000,
    "currency": "MXN",
    "success_url": "DIRECCIÓN QUE MUESTRA LA APP",
    "cancel_url": "DIRECCIÓN QUE MUESTRA LA APP"
  }'
```

La guía de Fintoc lista `amount`, `currency`, `success_url` y `cancel_url` como los parámetros obligatorios. Revisa su
[referencia de la API](https://docs.fintoc.com/api/payments-api/checkout-sessions/checkout-sessions-create) para cualquier
otro que tu cuenta necesite. La respuesta trae el `redirect_url` del checkout alojado y, para el Widget, el
`session_token`. Si el tuyo dice `session_token: null`, busca en la referencia el `ui_mode` que aplica a los pagos con el
Widget.

- Para las demos del **Widget**, pega el `session_token`.
- Para la demo del **checkout alojado**, usa las dos direcciones que muestra la pantalla como `success_url` y
  `cancel_url`, y pega el `redirect_url`.

Usa las [credenciales de prueba](https://docs.fintoc.com/guides/resources/test-mode) de Fintoc para completar un pago en
el sandbox.

## Qué se ha comprobado contra el sandbox

El SDK se construyó a partir de la documentación de Fintoc y se probó en un dispositivo. Ejecutar este ejemplo contra el
sandbox de Fintoc ha confirmado:

- **Eventos del Widget.** Con una llave `pk_test_` real el registro de eventos se llena, empezando por `opened`. El SDK
  siempre envía `_on_event=true`.
- **Un valor secreto en la dirección de regreso.** La demo del checkout alojado pone un valor aleatorio en ambas
  direcciones, y Fintoc devuelve el query string, así que el ejemplo distingue la dirección de éxito que lleva tu valor
  de una falsificada.
- **Un esquema personalizado como dirección de regreso.** Fintoc acepta `fintocsample://`. Una app real aun así debería
  preferir un App Link `https` de su propio dominio, que ninguna otra app puede reclamar.

Sigue abierto:

- **Cuándo una Checkout Session tiene un `session_token`.** La documentación de Fintoc muestra un `redirect_url` y un
  `session_token`, pero no cuándo el token es `null`. Revisa la respuesta de tu sesión y la referencia de la API de
  Fintoc para el `ui_mode` que aplica a los pagos con el Widget.

Cerrar la Custom Tab no informa nada a la app, por diseño: actualiza el estado desde tu backend.

## Cómo está construida

- Una sola Activity `singleTask` que maneja por sí misma los cambios de configuración, así que un Widget sobrevive a una
  rotación y un checkout alojado que regresa llega a la Activity que ya está abierta.
- `SampleSdk` es el único lugar que configura el SDK, y el selector de idioma lo vuelve a llamar.
- Textos en español, inglés, francés y portugués.
- La compilación release usa R8, así que cada compilación comprueba que el SDK funciona minificado.
- Pruebas: Robolectric para la lógica y las pantallas, y un dispositivo real para los enlaces de regreso, la pantalla
  propia del SDK y la accesibilidad (el Accessibility Test Framework de Google).
