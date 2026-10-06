package mx.dev1.fintoc.sdk

/** Fintoc environment a public key belongs to. Fintoc encodes it in the key prefix. */
public enum class FintocEnvironment {
    /** Sandbox: `pk_test_` keys. No real money moves. */
    TEST,

    /** Production: `pk_live_` keys. */
    LIVE,
}
