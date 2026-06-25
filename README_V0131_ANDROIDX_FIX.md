# SW Launcher v0.13.1 AndroidX Fix

Correção aplicada após migração para Kotlin + Jetpack Compose + Material 3.

## Corrigido

- Ativado `android.useAndroidX=true`
- Ativado `android.enableJetifier=true`
- Ajustado `org.gradle.jvmargs`
- Adicionado import `ContentScale` se necessário

Erro resolvido:

`Configuration :app:debugRuntimeClasspath contains AndroidX dependencies, but the android.useAndroidX property is not enabled`
