# SW Launcher v0.1

Primeira versão de um launcher Android real.

## O que já faz
- Aparece como opção de Tela Inicial/Home do Android.
- Lista os apps instalados que aparecem na gaveta.
- Abre apps ao tocar.
- Segurar em um app abre a tela de informações/configurações dele.
- Interface escura simples com relógio.

## Como compilar
1. Abra o projeto no Android Studio.
2. Deixe o Android Studio baixar o Gradle e o SDK necessário.
3. Conecte o celular com Depuração USB, ou gere o APK em: Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Instale no celular.
5. Aperte o botão Home do Android e escolha "SW Launcher" como tela inicial.

## Próximas versões sugeridas
- Barra de busca.
- Dock fixo com apps favoritos.
- Pasta de apps.
- Papel de parede customizado.
- Ocultar apps.
- Gestos.
- Grade ajustável.
- Sistema de temas.

## Compilar usando só celular

Veja o arquivo `README_MOBILE.md`.
Este projeto inclui o workflow `.github/workflows/build-apk.yml`, que gera o APK automaticamente pelo GitHub Actions.
