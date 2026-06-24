# SW Launcher v0.1 - Compilar APK usando só celular

Este projeto já vem com GitHub Actions configurado para gerar o APK na nuvem.
Você não precisa de Android Studio nem PC.

## O que você vai usar
- Celular Android
- Conta no GitHub
- Termux ou navegador
- O arquivo `.zip` deste projeto

## Caminho recomendado: GitHub Actions

### 1. Crie um repositório no GitHub
Crie um repositório chamado, por exemplo:

`SWLauncher`

Pode ser público ou privado.

### 2. Envie os arquivos do projeto
Você pode enviar pelo navegador ou pelo Termux.

No Termux, exemplo:

```bash
pkg update -y
pkg install git unzip -y
cd /sdcard/Download
unzip SWLauncher_v0_1_mobile.zip
cd SWLauncher_v0_1_mobile

git init
git add .
git commit -m "primeira versão mobile do SW Launcher"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/SWLauncher.git
git push -u origin main
```

Troque `SEU_USUARIO` pelo seu usuário do GitHub.

### 3. Gere o APK
No GitHub:

1. Abra o repositório.
2. Entre em **Actions**.
3. Abra **Build SW Launcher APK**.
4. Toque em **Run workflow**.
5. Depois que terminar, entre na execução.
6. Baixe o arquivo em **Artifacts**.

O APK vai estar dentro do artifact chamado:

`SWLauncher-v0.1-debug-apk`

### 4. Instale no celular
Depois de baixar o APK:

1. Toque no APK.
2. Permita instalar apps desconhecidos, se o Android pedir.
3. Instale.
4. Aperte o botão Home.
5. Escolha **SW Launcher**.

## Observações

- Esta é uma versão debug, perfeita para teste.
- Para publicar na Play Store, precisa gerar versão release assinada.
- Como launcher real, ele aparece como opção de tela inicial do Android.
- Esta v0.1 ainda é simples: lista apps e abre apps.

## Próximos recursos

- Dock inferior com favoritos.
- Pesquisa de apps.
- Pastas.
- Ocultar apps.
- Temas visuais.
- Papel de parede.
- Gestos.
- Layout estilo launcher premium.
