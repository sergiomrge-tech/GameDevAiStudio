# GameDev AI Studio — checkpoint para retomada

**Checkpoint:** quarta-feira, 14/10/2026  
**Retomada combinada:** sábado, 17/10/2026, depois das 21h (horário de Brasília, UTC-3), quando o usuário informa que seu limite do Claude Pro será renovado.  
**Repositório principal:** https://github.com/sergiomrge-tech/GameDevAiStudio  
**Branch canônica:** `main`  
**Commit-base confirmado antes deste checkpoint:** `a46191595f7c42e306f284a5c48b1349ee09afb3`

## Objetivo do projeto

Um aplicativo **Android nativo e mobile-first**, independente do GitHub no uso diário, para pedir melhorias em jogos, acompanhar trabalhos de especialistas, visualizar relatórios de qualidade e, em uma fase posterior, receber APKs verificáveis de projetos Godot/Unity. Os repositórios no GitHub são a base dos projetos; o PC pessoal não deve precisar ficar ligado durante o dia.

Prioridades permanentes: **trabalho real em código**, respeito aos assets e à identidade visual aprovados, testes concretos no Godot/Unity, nenhum mockup apresentado como captura real, checkpoints verificáveis e entregas de APK apenas após compilação efetiva.

## O que foi realmente implementado e verificado

1. Aplicativo Android **GameDev AI Studio v0.2.1** em `android/`, Java nativo, com telas **Início / Solicitar / Tarefas / Acompanhamento / Ajustes**. Token GitHub Fine-grained armazenado de forma criptografada por Android Keystore. Comunicação com GitHub API por HTTPS.
2. O aplicativo cria Issues `[JOGO]` no repositório do estúdio, consulta tarefas e recebe comentários dos agentes pelo próprio app.
3. Workflow de Issues `.github/workflows/issue-plan.yml` classifica solicitações para seis perfis (direção, programação, arte, gameplay, QA e builds). O planejamento por IA usa **OpenAI Responses API**, quando existe a secret `OPENAI_API_KEY`. Modelo padrão no código: `gpt-5-mini`, com `OPENAI_MODEL` opcional.
4. Versão v0.2.1 melhorou mensagens HTTP 401/403/404/422, limites e redes, acrescentou cópia do diagnóstico e preservação de rascunhos.
5. CI do repositório e pipeline Android (Gradle, JUnit, lint, assembleDebug) foram aprovados em 14/10/2026. O pipeline Android não corresponde a teste em dispositivo físico; não extrapolar.
6. O erro HTTP 403 inicial do aparelho foi resolvido após configurar corretamente o token com acesso **Issues: Read and write** ao repositório `GameDevAiStudio`. **Teste real bem-sucedido:** Issue [#4 — Combate](https://github.com/sergiomrge-tech/GameDevAiStudio/issues/4) criada pelo aplicativo em nome do jogo *Dungeons Fires*, engine Godot. O [workflow correspondente](https://github.com/sergiomrge-tech/GameDevAiStudio/actions/runs/38639103547) terminou com sucesso, e o comentário atribuiu a tarefa ao **Programador Sênior**.

## Estado no momento do checkpoint

- `main` confirmado com commit `a461915...`, que simplificou a ativação da API OpenAI para requerer somente a secret `OPENAI_API_KEY` (modelo padrão `gpt-5-mini`).
- O usuário **não confirmou ter cadastrado** `OPENAI_API_KEY`; a Issue #4 responde **"Planejamento IA pendente"**. Não presumir que há chave, billing ou créditos.
- O usuário **assina Claude Pro**, mas informou que **o limite de uso só volta no sábado, 17/10/2026, às 21h**. Claude Pro **não inclui crédito da API Anthropic**; uso da assinatura em Claude Code oficial é separado da API.
- O fluxo do estúdio hoje é **triagem e eventual planejamento**, **não** execução autônoma de mudanças nos repositórios de jogos. Não chamar os seis perfis de executores independentes já prontos.
- O workflow de GitHub pode hospedar testes/compilações e responder Issues, mas não substitui modelo de programação. Uma integração com Claude Code deve respeitar os mecanismos oficiais, permissões, limitação de uso e confirmação do usuário. **Não** desviar sessão/cookies/tokens Claude Pro como se fossem API.
- O repositório do estúdio é **público**: Issues e comentários são públicos. Nunca registrar tokens, credenciais, segredos ou dados confidenciais neles.
- O APK de teste já foi publicado na tag `v0.2.1-preview`; builds debug do GitHub podem ser assinados com chaves diferentes, exigindo reinstalação. Não alegar que o APK mais recente corresponde ao último commit se não for verificado.

## Plano de retomada — sábado

1. **Primeiro:** confirmar com o usuário se a cota do **Claude Pro/Claude Code** foi renovada; não supor renovação antecipada.
2. Verificar diretamente a branch `main`, workflows, Issues existentes (especialmente #4), e disponibilidade de acesso de ferramentas GitHub/Claude. Preservar o código existente.
3. Priorizar um fluxo legítimo **GameDev AI Studio → Claude Code oficial**, preferencialmente com deep link e tarefa/repositório preparados, explicitando que iniciar a execução requer confirmação no Claude, que não há API incluída no Pro e que a execução fica sujeita a seus limites.
4. Em paralelo, desenhar/implementar o **executor real de projeto Godot** via ambiente autorizado, com branches por tarefa, inspeção do repositório do jogo, testes automatizados, relatórios, build de APK somente após compilação e revisão. Evitar colocar credenciais de longa duração no app/workflows sem necessidade.
5. Utilizar **Dungeons Fire** e a tarefa **#4 Combate** como primeiro piloto, mas descobrir/validar o repositório correto do jogo antes de editar qualquer projeto. Fazer revisão de escopo, preservar estilo visual acordado; não alegar alterações se só houver planejamento.
6. Retomar do checkpoint sem pedir ao usuário que reexplique a arquitetura. Entregar mudanças reais testadas e um relatório com limites e links GitHub.

## Ponto de partida recomendado na próxima conversa

> "Continue o GameDev AI Studio a partir do checkpoint de 14/10. Minha cota do Claude Pro voltou. Integre o fluxo oficial de Claude Code e avance para a execução real de tarefas Godot, começando por Dungeons Fire / Issue #4."

**Nota operacional:** salvar este checkpoint não cria trabalho em segundo plano, não reserva créditos Claude e não agenda nenhuma execução automática. A continuação depende de o usuário retomar a conversa.
