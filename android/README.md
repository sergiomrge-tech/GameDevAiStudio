# Android client — GameDev AI Studio v0.2

Este projeto **é um app Android nativo** (Java, Android SDK; sem WebView e sem dependências de UI externas).

## Arquitetura

- Interface Android: Início, Solicitar, Tarefas, Acompanhamento e Ajustes.
- Token GitHub salvo somente no aparelho e cifrado com Android Keystore AES-256-GCM.
- Cliente HTTPS se comunica exclusivamente com `https://api.github.com`.
- Uma solicitação vira uma Issue pública do repositório central com prefixo `[JOGO]`.
- GitHub Actions responde à Issue com classificação de especialista e, se configurada a API OpenAI, um plano.
- Atualização do histórico e das respostas é **manual** pelo app, sem serviço em segundo plano.
- Nenhum token, código-fonte privado ou senha é embutido no APK.

## Conectar uma vez e depois usar só o app

1. Instale o APK da seção Releases do repositório.
2. Em Ajustes, toque em *Abrir criação de token no GitHub* (essa etapa única abre o navegador).
3. No GitHub crie um **Fine-grained personal access token** com:
   - Resource owner: `sergiomrge-tech`
   - Repository access: **Only select repositories** > `GameDevAiStudio`.
   - Repository permissions: **Issues > Read and write**. Metadata read será concedido automaticamente.
   - Prazo curto (ex.: 30 ou 90 dias); revogue quando não usar.
4. Copie o token no aplicativo e toque em **Validar e conectar**.
5. Abra **Solicitar**, informe jogo, engine, título e pedido.
6. Abra **Tarefas**, escolha a tarefa e use **Verificar novas respostas**.

O projeto deve permitir o acesso à criação de Issues. O workflow `.github/workflows/issue-plan.yml` precisa estar ativo no GitHub. Se OPENAI_API_KEY e OPENAI_MODEL não estiverem configurados no repositório, a resposta automática fará apenas a triagem determinística — isso é esperado, não uma falha.

**Privacidade**: Este repositório é público; Issues e comentários também são públicos. Não coloque segredos, dados pessoais sensíveis ou propriedade intelectual privada no texto das tarefas.

## Build na nuvem

O workflow `.github/workflows/android.yml` roda testes JUnit, Android Lint e `assembleDebug` com Gradle 8.9, Java 17 e Android Gradle Plugin 8.7.3. A cada aprovação na branch main, publica a prévia no GitHub Releases.

Este APK é **debug**: a assinatura é temporária e pode mudar a cada build. Para instalar uma nova prévia, pode ser necessário desinstalar a anterior. Faça backup dos dados relevantes antes; o token é removido ao desinstalar.

## Limitações explícitas

- Ainda não é um motor autônomo de desenvolvimento de jogos: o workflow classifica e planeja.
- IA de verdade exige API configurada e possíveis custos.
- Não há OAuth de um clique; a criação do token é inicial/manual.
- Não há push/merge automatizado de jogos, execução remota Godot, screenshot ou APK de jogos.
- O app não está publicado na Play Store; distribuição de teste por APK no GitHub.
