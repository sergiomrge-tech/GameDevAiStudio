package com.gamedev.aistudio;

/** User-safe and actionable API diagnostics; never includes bearer tokens. */
public final class GitHubApiErrors {
    private GitHubApiErrors() {}

    public static String describe(int status, String githubMessage, String remaining) {
        String message = githubMessage == null ? "" : githubMessage.trim();
        if(message.length()>220) message=message.substring(0,220)+"...";
        if(status==401) {
            return "HTTP 401 — Token inválido ou expirado. Abra Ajustes e conecte um novo token GitHub.";
        }
        if(status==403 && "0".equals(remaining)) {
            return "HTTP 403 — Limite de chamadas da API GitHub atingido. Tente novamente mais tarde.";
        }
        if(status==403) {
            return "HTTP 403 — A conta foi identificada, mas o token não tem acesso suficiente para criar a tarefa.\n\n"
                +"Abra GitHub > Settings > Developer settings > Personal access tokens > Fine-grained tokens. "
                +"Selecione o token do GameDev AI Studio e confira:\n"
                +"• Resource owner: sergiomrge-tech\n"
                +"• Repository access: GameDevAiStudio\n"
                +"• Repository permissions: Issues → Read and write\n"
                +"• Token ativo, não expirado e aprovado, quando necessário.\n\n"
                +"Depois volte aos Ajustes do aplicativo para salvar o token com as permissões corretas."
                +(message.isEmpty()?"":"\n\nGitHub: "+message);
        }
        if(status==404) {
            return "HTTP 404 — Repositório indisponível para esta credencial. Confirme que o token dá acesso ao repositório sergiomrge-tech/GameDevAiStudio e que Issues está habilitado.";
        }
        if(status==422) {
            return "HTTP 422 — O GitHub rejeitou os dados da tarefa. Revise título e descrição."
                +(message.isEmpty()?"":"\nGitHub: "+message);
        }
        if(status==429) {
            return "HTTP 429 — Muitas chamadas à API. Aguarde antes de tentar novamente.";
        }
        if(status>=500) {
            return "HTTP "+status+" — Instabilidade temporária na API do GitHub. Tente novamente mais tarde.";
        }
        return "HTTP "+status+" — Falha na API do GitHub."
            +(message.isEmpty()?"":"\nGitHub: "+message);
    }

    public static String network(Exception error) {
        Throwable cause=error;
        while(cause.getCause()!=null && cause.getCause()!=cause) cause=cause.getCause();
        if(cause instanceof java.net.UnknownHostException) {
            return "Falha de conexão: não foi possível localizar api.github.com. Confira internet, VPN e DNS.";
        }
        if(cause instanceof java.net.SocketTimeoutException) {
            return "Tempo esgotado para falar com o GitHub. Confira a conexão e tente novamente.";
        }
        if(cause instanceof javax.net.ssl.SSLException) {
            return "Falha na conexão HTTPS com o GitHub. Confira data, hora e rede do aparelho.";
        }
        if(cause instanceof java.io.IOException) {
            return "Não foi possível conectar à API do GitHub. Confira a internet e tente novamente. Detalhe: "
                +cause.getClass().getSimpleName();
        }
        return error.getMessage()==null?"Erro inesperado na integração GitHub.":error.getMessage();
    }
}
