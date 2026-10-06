# App de exemplo

[English](../en/sample-app.md) · [Español](../es/sample-app.md) · [Français](../fr/sample-app.md) · [Voltar ao README](../../README.md)

O módulo `app` é um pequeno app Compose que usa cada parte do SDK, para você experimentá-lo contra o sandbox da Fintoc.
Ele **não guarda nenhuma chave secreta** e não consegue criar session tokens: quem faz isso é o seu backend, como em um
app real. O exemplo pede que você cole o que o seu backend lhe daria.

## Execute

1. Pegue sua chave pública de sandbox (`pk_test_…`) no dashboard da Fintoc.
2. Adicione-a ao `local.properties`, que o Git ignora:

```properties
fintoc.publicKey=pk_test_your_key
```

3. Execute `./gradlew :app:installDebug`, ou rode o módulo `app` pelo Android Studio.

A compilação recusa uma chave secreta (`sk_…`) e qualquer coisa que não pareça `pk_test_…` ou `pk_live_…`. Sem chave, o app
usa uma de preenchimento e avisa na tela inicial, e o Widget não consegue iniciar fluxos reais.

## O que cada demo mostra

| Demo | API do SDK | O que precisa |
|---|---|---|
| Conectar uma conta bancária | `FintocWidget` com `Movements` | Apenas sua chave pública |
| Pagar com o Widget | `FintocWidget` com `Payments` | Um session token |
| Pagar com um provedor de token | `FintocWidget` com um provedor `suspend` | Um session token. Um backend simulado o entrega após uma pausa, e pode falhar uma vez para mostrar a nova tentativa. |
| Pagar em uma tela própria | `FintocWidgetContract` | Um session token |
| Checkout hospedado | `FintocHostedCheckout` | Um `redirect_url` |

Sob cada Widget, o exemplo lista os eventos que ele informa. Nunca imprime tokens: uma conexão bancária apenas diz que um
exchange token chegou. O seletor de idioma da tela inicial define `FintocConfiguration.language`, que muda os textos
próprios do SDK.

## Obtenha um session token ou uma URL de redirecionamento

Crie uma Checkout Session no seu terminal com a sua chave **secreta**. A chave fica no seu shell e nunca entra no app. O
`amount` vai na menor unidade da moeda, então `1000` são MXN 10,00:

```bash
read -rs FINTOC_SECRET_KEY    # digite ou cole sua chave sk_test_… e tecle Enter
curl --request POST "https://api.fintoc.com/v2/checkout_sessions" \
  --header "Authorization: $FINTOC_SECRET_KEY" \
  --header "Content-Type: application/json" \
  --data-raw '{
    "amount": 1000,
    "currency": "MXN",
    "success_url": "ENDEREÇO MOSTRADO PELO APP",
    "cancel_url": "ENDEREÇO MOSTRADO PELO APP"
  }'
```

O guia da Fintoc lista `amount`, `currency`, `success_url` e `cancel_url` como os parâmetros obrigatórios. Consulte a sua
[referência da API](https://docs.fintoc.com/api/payments-api/checkout-sessions/checkout-sessions-create) para qualquer
outro que sua conta precise. A resposta traz o `redirect_url` do checkout hospedado e, para o Widget, o `session_token`.
Se o seu disser `session_token: null`, procure na referência o `ui_mode` que se aplica aos pagamentos com o Widget.

- Para as demos do **Widget**, cole o `session_token`.
- Para a demo do **checkout hospedado**, use os dois endereços que a tela mostra como `success_url` e `cancel_url`, e cole
  o `redirect_url`.

Use as [credenciais de teste](https://docs.fintoc.com/guides/resources/test-mode) da Fintoc para concluir um pagamento no
sandbox.

## O que executá-lo pode esclarecer

O SDK foi construído a partir da documentação da Fintoc e testado em um dispositivo, mas alguns comportamentos só se veem
com uma sessão real. O exemplo é o lugar para verificá-los:

- **Eventos do Widget.** Com uma chave `pk_test_` real o registro de eventos deve se encher, começando por `opened`. O SDK
  sempre envia `_on_event=true`: se os eventos não chegarem, comece por aí.
- **Um valor secreto no endereço de retorno.** A demo do checkout hospedado põe um valor aleatório nos dois endereços.
  Depois de pagar, o exemplo deve dizer que o endereço de sucesso voltou. Se a Fintoc rejeitar ou descartar a query
  string, o SDK precisaria de outra forma de levar o valor.
- **Um esquema personalizado como endereço de retorno.** O exemplo usa `fintocsample://`. Se a Fintoc o recusar, use um
  App Link `https` do seu próprio domínio.
- **Fechar a Custom Tab** não informa nada ao app, por design: atualize o status a partir do seu backend.

## Como é construído

- Uma única Activity `singleTask` que trata sozinha as mudanças de configuração, então um Widget sobrevive a uma rotação e
  um checkout hospedado que volta chega à Activity que já está aberta.
- `SampleSdk` é o único lugar que configura o SDK, e o seletor de idioma o chama de novo.
- Textos em português, inglês, espanhol e francês.
- O build release usa R8, então cada build verifica que o SDK funciona minificado.
- Testes: Robolectric para a lógica e as telas, e um dispositivo real para os links de retorno, a tela própria do SDK e a
  acessibilidade (o Accessibility Test Framework do Google).
