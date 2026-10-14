package com.gamedev.aistudio;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** HTTPS-only GitHub REST client: all requests go to hardcoded github.com APIs. */
final class GitHubService {
    static final String REPO="sergiomrge-tech/GameDevAiStudio";
    static final String OWNER="sergiomrge-tech";
    static final String BASE="https://api.github.com";
    private GitHubService(){}

    static JSONObject identity(String token) throws Exception {
        return new JSONObject(request("GET","/user",token,null));
    }

    static JSONArray tasks(String token) throws Exception {
        JSONArray all=new JSONArray(request("GET","/repos/"+REPO+
            "/issues?state=all&per_page=100&creator="+OWNER+"&sort=created&direction=desc",token,null));
        JSONArray selected=new JSONArray();
        for(int i=0;i<all.length();i++){
            JSONObject item=all.optJSONObject(i);
            if(item!=null&&!item.has("pull_request")&&item.optString("title","").startsWith("[JOGO]"))
                selected.put(item);
        }
        return selected;
    }

    static JSONArray comments(String token,int number) throws Exception {
        if(number<=0) throw new IllegalArgumentException("ID de tarefa inválido");
        return new JSONArray(request("GET","/repos/"+REPO+"/issues/"+number+
            "/comments?per_page=100",token,null));
    }

    /** Check Issues read access before saving a token; write permission is checked on POST. */
    static void verifyIssueRead(String token) throws Exception {
        request("GET","/repos/"+REPO+"/issues?state=open&per_page=1",token,null);
    }

    static JSONObject createTask(String token,String title,String body) throws Exception {
        JSONObject payload=new JSONObject();
        payload.put("title",title);
        payload.put("body",body);
        return new JSONObject(request("POST","/repos/"+REPO+"/issues",token,payload));
    }

    private static String request(String method,String path,String token,JSONObject payload) throws Exception {
        if(token==null||token.length()<20) throw new IllegalArgumentException("Conecte sua conta GitHub em Ajustes.");
        URL url=new URL(BASE+path);
        if(!"https".equals(url.getProtocol())||!"api.github.com".equals(url.getHost()))
            throw new SecurityException("Destino de API não permitido");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("User-Agent","GameDevAIStudio-Android/0.2");
        c.setUseCaches(false);
        try{
            if(payload!=null){
                byte[] bytes=payload.toString().getBytes(StandardCharsets.UTF_8);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                c.setFixedLengthStreamingMode(bytes.length);
                try(OutputStream out=c.getOutputStream()){out.write(bytes);}
            }
            int status=c.getResponseCode();
            InputStream stream=status>=400?c.getErrorStream():c.getInputStream();
            String response=readLimited(stream,2*1024*1024);
            if(status>=400){
                String message="HTTP "+status;
                try{
                    String detail=new JSONObject(response).optString("message","");
                    if(!detail.isEmpty()) message+=": "+detail;
                }catch(Exception ignored){}
                throw new IllegalStateException(GitHubApiErrors.describe(status,
                    message.startsWith("HTTP "+status+": ")?
                        message.substring(("HTTP "+status+": ").length()):message,
                    c.getHeaderField("X-RateLimit-Remaining")));
            }
            return response;
        }catch(java.io.IOException io){
            throw new IllegalStateException(GitHubApiErrors.network(io),io);
        }finally{c.disconnect();}
    }

    private static String readLimited(InputStream stream,int maxBytes) throws Exception {
        if(stream==null)return "";
        try(InputStream input=stream;ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];
            int read;
            while((read=input.read(buf))!=-1){
                if(output.size()+read>maxBytes)throw new IllegalStateException("Resposta do GitHub muito grande");
                output.write(buf,0,read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
