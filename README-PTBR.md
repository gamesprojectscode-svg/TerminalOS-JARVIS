# Terminal OS 2.0 — JARVIS Mobile

Projeto Android nativo para Samsung Galaxy A06 sem root, com UI inspirada em painéis de ficção científica (sem copiar logos/personagens de terceiros).

## Recursos implementados
- WebView com interface responsiva, logo vetorial e abas Terminal, Code Studio, Arquivos, Lembretes e Sistema.
- Terminal local com `help`, `date`, `device`, `pwd`, `ls`, `mkdir`, `touch`, `cat`, `write`, `rm`, `ipconfig`, `ifconfig`, `wifi`, `bluetooth`, `settings`, `python`, `reminder`, `clear`.
- Workspace privado do app: comandos de arquivo não acessam arbitrariamente todo o armazenamento.
- Editor Python com runtime embutido Chaquopy; botão Executar roda Python no app.
- Playground HTML/JavaScript local em iframe com sandbox; CSS é aplicado como parte de HTML.
- Lembretes via AlarmManager e notificação Android. Permissão de notificação é solicitada em Android 13+.
- Workflow GitHub Actions em `.github/workflows/build-apk.yml`.

## Compilar na nuvem
1. Envie todo o conteúdo do ZIP para a raiz do repositório GitHub, mantendo os caminhos.
2. Abra Actions > Build Terminal OS APK > Run workflow (ou faça push para `main`).
3. Espere o job ficar verde e baixe o artifact `TerminalOS-APK`.
4. Extraia o ZIP do artifact e instale `app-debug.apk`.

## Limites importantes
- Sem root, o app não pode ler diretamente senhas Wi-Fi salvas, executar shell arbitrário com privilégios, alterar livremente Wi-Fi/Bluetooth, nem acessar arquivos privados de outros apps.
- `ipconfig` mostra apenas informações que o Android disponibiliza ao app; gateway/DNS podem estar ocultos dependendo da versão.
- Python embutido não inclui automaticamente todas as bibliotecas de terceiros. `exec` não é um sandbox de segurança forte; rode somente código em que confia.
- O editor não é um IDE desktop completo: não inclui depurador, autocomplete robusto, Git integrado ou compiladores C/C++/Java. JavaScript/HTML executam no WebView; CSS é linguagem de estilo e é interpretada pelo navegador.
- Lembretes usam alarmes simples; políticas de bateria/fabricante e reinicialização podem afetar entrega. Conceda notificações e remova restrições de bateria se necessário.
- APK debug não é assinado para distribuição pública. Para publicar, configure assinatura release própria.
