# GameDev AI Studio — v0.1.0

Painel mobile-first e backend Python para coordenar seis especialistas em desenvolvimento de jogos. **Este é um MVP real, não uma suíte autônoma completa.**

## O que funciona

- Dashboard web responsivo em português, acessível pelo navegador Android.
- Cadastro de projetos GitHub e associação opcional a checkout Git local.
- Persistência SQLite de projetos, tarefas, estado, planos e logs.
- Encaminhamento de tarefas para seis perfis: direção, programação, arte, gameplay, QA e builds.
- Planejamento real via OpenAI Responses API, apenas quando OPENAI_API_KEY e OPENAI_MODEL estão configurados.
- Execução opcional com Codex CLI em repositório local limpo, criando branch por tarefa; sem commit/push/merge automáticos.
- Adaptador para checagem Godot headless em runner que tenha o executável instalado.
- Testes Python, HTTP API e workflow GitHub Actions.

## Como iniciar (Python 3.11+)

    git clone https://github.com/sergiomrge-tech/GameDevAiStudio.git
    cd GameDevAiStudio
    python -m unittest discover -s tests -v
    python -m studio.server

No mesmo computador/servidor, abra http://127.0.0.1:8765.

Para usar planejamento por IA, configure OPENAI_API_KEY e OPENAI_MODEL no ambiente do processo. A assinatura do ChatGPT não fornece automaticamente créditos de API.

## Enviar tarefas diretamente do celular via GitHub Issues

Sem precisar de servidor ligado: abra a aba Issues do repositório, toque em New issue e escolha o formulário Tarefa de jogo (título começando por [JOGO]). O GitHub Actions identifica o especialista e responde em comentário. O workflow aceita apenas Issues abertas pelo proprietário. Fechar e reabrir a Issue permite novo processamento depois de configurar a IA.

Para planejamento real por IA, configure Settings > Secrets and variables > Actions > Secrets: OPENAI_API_KEY; em Variables: OPENAI_MODEL (um modelo permitido na sua conta). Sem configuração há somente triagem, não um plano inventado. A API é cobrada separadamente do ChatGPT Plus.

O conteúdo das Issues deste repositório público também é público. Nunca publique segredos. Esta automação não edita código, executa Godot ou gera APK.

## Acesso no Android

O painel **não fica disponível publicamente só porque o repositório está no GitHub**. É necessário hospedar o processo Python em um computador/servidor ligado. Para acesso remoto, use reverse proxy HTTPS, autenticação e configure STUDIO_HOST=0.0.0.0, STUDIO_TOKEN com valor forte. A interface recebe o token e o guarda apenas na sessão do navegador. Não exponha o servidor sem HTTPS e controles de acesso adicionais.

## Execução de tarefas com Codex

Por segurança ENABLE_CODEX_EXEC=0 inicialmente. Em um runner isolado:

1. Instale git e Codex CLI, configure a autenticação do Codex sem expor tokens na UI.
2. Defina STUDIO_WORKSPACE para uma pasta de trabalho dedicada e STUDIO_TOKEN obrigatório.
3. Clone o repositório de cada jogo dentro desse workspace (exemplo: workspace/dungeons-fire).
4. No formulário de projetos, informe o campo local_path via API para vincular ao checkout; o painel cria um caminho padrão pelo nome quando não informado.
5. Habilite ENABLE_CODEX_EXEC=1 somente após proteger o runner e conhecer os riscos.
6. A ação Executar cria branch studio/task-<id> e chama codex exec com sandbox workspace-write. Revise os arquivos alterados antes de commitar ou abrir PR.

O projeto **não clona repositórios automaticamente** e **não gera APK automaticamente**. Builds Android, revisão visual com GPU e publicação são etapas futuras.

## API

- GET /api/health, GET /api/agents
- GET /api/projects, POST /api/projects
- GET /api/tasks, GET /api/tasks/<id>, POST /api/tasks
- POST /api/tasks/<id>/plan, POST /api/tasks/<id>/run

POST /api/projects recebe name, repository (opcional) e local_path (opcional, restrito a STUDIO_WORKSPACE). POST /api/tasks recebe project_id, title e description. Requisições autenticadas usam cabeçalho X-Studio-Token.

## Validação Godot

Com Godot disponível no PATH:

    python scripts/check_godot.py /caminho/do/jogo --godot godot

Apenas importa e verifica que o projeto abre no modo headless; não substitui testes de renderização, gameplay ou Android.

## Estrutura

- studio/core.py: banco SQLite, regras de roteamento, planejamento e executor
- studio/server.py: HTTP JSON e hospedagem do dashboard
- web/index.html: interface mobile
- scripts/check_godot.py: verificação headless opcional
- tests/: testes de regressão
- .github/workflows/ci.yml: pipeline CI
- AGENTS.md: normas permanentes dos agentes

## Segurança e limites

Servidor local por padrão; rede requer token + HTTPS via proxy. Não publique .env, segredos ou banco de dados. O runner com Codex pode executar código gerado por IA; execute-o em VM/container isolado, com credenciais mínimas. Falha de planejamento não é simulação de resposta. Não há integração nativa com Blender, Unity, geração APK ou QA visual no MVP.

## Próximos marcos

v0.2: importação/clonagem GitHub com credenciais restritas, inspeção automatizada do Godot, pipeline de builds Android, histórico de diffs e PRs; v0.3: executor visual dedicado, assets Blender, biblioteca artística; v1.0: orquestração robusta multiagente e observabilidade.
