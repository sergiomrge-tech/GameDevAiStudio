package com.gamedev.aistudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android mobile-first client; no API keys embedded in the APK. */
public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(9,16,32);
    private static final int PANEL=Color.rgb(18,30,51);
    private static final int SUBPANEL=Color.rgb(24,39,65);
    private static final int BORDER=Color.rgb(43,62,91);
    private static final int WHITE=Color.rgb(244,247,255);
    private static final int MUTED=Color.rgb(160,178,207);
    private static final int BLUE=Color.rgb(111,139,255);
    private static final int MINT=Color.rgb(103,221,183);
    private static final int GOLD=Color.rgb(229,189,115);
    private static final String[] ENGINES={"Godot","Unity","Unreal Engine","Blender / Assets","Outra"};

    private enum Page { HOME, NEW_TASK, TASKS, DETAIL, SETTINGS }
    private Page page=Page.HOME;
    private LinearLayout root,body;
    private JSONArray taskCache=new JSONArray();
    private JSONArray commentCache=new JSONArray();
    private JSONObject detailIssue;
    private boolean loadingTasks=false,loadingComments=false, submitting=false;
    private final String[] draft=new String[5];
    private final ExecutorService executor=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        page=hasToken()?Page.HOME:Page.SETTINGS;
        render();
        if(hasToken()) loadTasks(false);
    }

    @Override protected void onDestroy(){
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed(){
        if(page==Page.DETAIL){navigate(Page.TASKS);return;}
        if(page!=Page.HOME){navigate(Page.HOME);return;}
        super.onBackPressed();
    }

    private int dp(float value){return (int)(getResources().getDisplayMetrics().density*value+0.5f);}
    private boolean hasToken(){return !SecureTokenStore.read(this).isEmpty();}
    private GradientDrawable background(int color,int radius,int border){
        GradientDrawable gd=new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radius));
        if(border!=0)gd.setStroke(dp(1),border);
        return gd;
    }
    private GradientDrawable gradient(){
        GradientDrawable gd=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(40,56,107),Color.rgb(22,36,66),Color.rgb(18,29,51)});
        gd.setCornerRadius(dp(23));
        gd.setStroke(dp(1),Color.rgb(71,91,151));
        return gd;
    }
    private LinearLayout column(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout row(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setIncludeFontPadding(true);
        if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        v.setLineSpacing(dp(2),1f);
        return v;
    }
    private void gap(LinearLayout parent,int height){
        View v=new View(this);
        parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
    }
    private void add(LinearLayout parent,View child){parent.addView(child);}
    private LinearLayout card(LinearLayout parent){
        LinearLayout item=column();
        item.setPadding(dp(18),dp(18),dp(18),dp(18));
        item.setBackground(background(PANEL,19,BORDER));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(14);
        parent.addView(item,p);
        return item;
    }
    private TextView heading(LinearLayout target,String title,String subtitle){
        TextView h=text(title,25,WHITE,true);
        add(target,h);
        if(subtitle!=null&&!subtitle.isEmpty()){
            gap(target,6);
            add(target,text(subtitle,13,MUTED,false));
        }
        gap(target,20);
        return h;
    }
    private Button button(String label,boolean primary){
        Button b=new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(primary?Color.WHITE:WHITE);
        b.setPadding(dp(17),dp(11),dp(17),dp(11));
        b.setMinHeight(dp(52));
        b.setBackground(background(primary?Color.rgb(78,104,236):SUBPANEL,13,primary?0:BORDER));
        b.setElevation(0);
        return b;
    }
    private void fullButton(LinearLayout parent,Button b){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));
        p.topMargin=dp(11);
        parent.addView(b,p);
    }
    private EditText field(LinearLayout parent,String title,String hint,boolean multi){
        add(parent,text(title,12,MUTED,true));
        gap(parent,7);
        EditText edit=new EditText(this);
        edit.setTextSize(15);
        edit.setTextColor(WHITE);
        edit.setHintTextColor(Color.rgb(102,121,156));
        edit.setHint(hint);
        edit.setPadding(dp(14),dp(12),dp(14),dp(12));
        edit.setBackground(background(Color.rgb(12,22,40),11,BORDER));
        if(multi){
            edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            edit.setGravity(Gravity.TOP|Gravity.START);
            edit.setMinLines(4);
            edit.setMaxLines(9);
        }else{
            edit.setSingleLine(true);
            edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        parent.addView(edit,new LinearLayout.LayoutParams(-1,-2));
        gap(parent,15);
        return edit;
    }
    private void pill(LinearLayout parent,String label,int color){
        TextView v=text(label,11,color,true);
        v.setPadding(dp(12),dp(7),dp(12),dp(7));
        v.setBackground(background(SUBPANEL,18,0));
        add(parent,v);
    }
    private void render(){
        root=column();
        root.setBackgroundColor(BG);
        root.setFocusableInTouchMode(true);
        if(Build.VERSION.SDK_INT>=30){
            root.setOnApplyWindowInsetsListener((view,insets)->{
                android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(i.left,i.top,i.right,i.bottom);
                return insets;
            });
        }
        setContentView(root);
        renderHeader();

        ScrollView scrolling=new ScrollView(this);
        scrolling.setFillViewport(true);
        scrolling.setClipToPadding(false);
        scrolling.setVerticalScrollBarEnabled(false);
        body=column();
        body.setPadding(dp(18),dp(19),dp(18),dp(28));
        scrolling.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scrolling,new LinearLayout.LayoutParams(-1,0,1f));

        switch(page){
            case HOME: renderHome();break;
            case NEW_TASK: renderNewTask();break;
            case TASKS: renderTasks();break;
            case DETAIL: renderDetail();break;
            case SETTINGS: renderSettings();break;
        }
        renderBottomNav();
    }
    private void renderHeader(){
        LinearLayout top=row();
        top.setPadding(dp(18),dp(13),dp(18),dp(12));
        top.setBackgroundColor(Color.rgb(14,24,43));
        TextView emblem=text("◆",23,BLUE,true);
        emblem.setGravity(Gravity.CENTER);
        emblem.setBackground(background(Color.rgb(33,46,85),12,Color.rgb(58,79,138)));
        top.addView(emblem,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout names=column();
        names.setPadding(dp(11),0,0,0);
        add(names,text("GAMEDEV  AI STUDIO",15,WHITE,true));
        add(names,text("Seu estúdio de jogos",11,MUTED,false));
        top.addView(names,new LinearLayout.LayoutParams(0,-2,1f));
        TextView version=text("v0.2",11,MINT,true);
        version.setPadding(dp(10),dp(7),dp(10),dp(7));
        version.setBackground(background(SUBPANEL,12,0));
        add(top,version);
        root.addView(top);
    }

    private void navButton(LinearLayout bar,String icon,String name,Page target){
        LinearLayout item=column();
        item.setGravity(Gravity.CENTER);
        boolean active=page==target ||(target==Page.TASKS&&page==Page.DETAIL);
        add(item,text(icon,22,active?BLUE:MUTED,true));
        TextView label=text(name,10,active?WHITE:MUTED,active);
        label.setGravity(Gravity.CENTER);
        add(item,label);
        item.setOnClickListener(v->navigate(target));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(66),1f);
        bar.addView(item,p);
    }
    private void renderBottomNav(){
        LinearLayout bar=row();
        bar.setPadding(dp(7),dp(2),dp(7),dp(2));
        bar.setBackground(background(Color.rgb(16,27,48),0,BORDER));
        navButton(bar,"⌂","Início",Page.HOME);
        navButton(bar,"✦","Solicitar",Page.NEW_TASK);
        navButton(bar,"▤","Tarefas",Page.TASKS);
        navButton(bar,"⚙","Ajustes",Page.SETTINGS);
        root.addView(bar);
    }
    private void navigate(Page next){
        page=next;
        render();
        if(next==Page.TASKS&&hasToken()&&!loadingTasks)loadTasks(true);
    }

    private void renderHome(){
        add(body,text("QUARTEL-GENERAL  /  MOBILE",11,GOLD,true));
        gap(body,9);
        heading(body,"Seu próximo jogo começa aqui.",
            "Peça melhorias, acompanhe os especialistas e revise o resultado sem abrir o computador.");
        LinearLayout hero=card(body);
        hero.setBackground(gradient());
        add(hero,text("✦  ESTÚDIO CONECTADO",12,MINT,true));
        gap(hero,17);
        add(hero,text("Transforme ideias em tarefas reais.",23,WHITE,true));
        gap(hero,9);
        add(hero,text("Uma ordem pelo celular. Acompanhamento no próprio aplicativo.",13,MUTED,false));
        gap(hero,10);
        Button start=button("Nova solicitação   →",true);
        fullButton(hero,start);
        start.setOnClickListener(v->navigate(hasToken()?Page.NEW_TASK:Page.SETTINGS));
        gap(body,5);
        LinearLayout summaries=row();
        LinearLayout one=summary("06","Especialistas");
        LinearLayout two=summary(String.valueOf(taskCache.length()),"Solicitações");
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1f);
        left.rightMargin=dp(10);
        summaries.addView(one,left);
        summaries.addView(two,new LinearLayout.LayoutParams(0,-2,1f));
        add(body,summaries);
        gap(body,20);
        add(body,text("Status do sistema",18,WHITE,true));
        gap(body,11);
        LinearLayout status=card(body);
        add(status,text(hasToken()?"●  GitHub configurado":"○  GitHub ainda não conectado",14,
            hasToken()?MINT:GOLD,true));
        gap(status,8);
        add(status,text(hasToken()?
           "Pronto para criar tarefas e consultar respostas no repositório.":
           "Configure uma credencial GitHub uma única vez para enviar suas tarefas.",13,MUTED,false));
        if(!hasToken()){
            Button configure=button("Configurar acesso",false);
            fullButton(status,configure);
            configure.setOnClickListener(v->navigate(Page.SETTINGS));
        }
        LinearLayout note=card(body);
        add(note,text("Como funciona a equipe?",16,WHITE,true));
        gap(note,8);
        add(note,text("A solicitação gera uma Issue no GitHub. O workflow identifica o especialista e comenta a triagem. Planejamento por IA só funciona se a API estiver ativada no repositório.",13,MUTED,false));
        gap(note,11);
        add(note,text("Importante: nesta versão ainda não há execução automática do jogo ou geração de APK de jogos.",12,GOLD,false));
    }
    private LinearLayout summary(String count,String label){
        LinearLayout s=column();
        s.setPadding(dp(16),dp(15),dp(16),dp(15));
        s.setBackground(background(PANEL,16,BORDER));
        add(s,text(count,26,WHITE,true));
        add(s,text(label,11,MUTED,false));
        return s;
    }

    private void renderNewTask(){
        heading(body,"Nova solicitação","Descreva a mudança. O diretor encaminhará para o especialista adequado.");
        if(!hasToken()){
            LinearLayout warn=card(body);
            add(warn,text("Conecte o GitHub antes de enviar uma tarefa.",13,GOLD,true));
            Button setup=button("Ir para Ajustes",true);
            fullButton(warn,setup);
            setup.setOnClickListener(v->navigate(Page.SETTINGS));
            return;
        }
        LinearLayout form=card(body);
        EditText project=field(form,"JOGO","Ex.: Dungeons Fire",false);
        EditText engine=field(form,"ENGINE","Godot, Unity ou Unreal",false);
        engine.setText("Godot");
        EditText title=field(form,"TÍTULO DA TAREFA","Ex.: Corrigir animação de corrida",false);
        EditText description=field(form,"O QUE DEVE SER FEITO?","Explique o problema, o que quer mudar e os detalhes importantes...",true);
        EditText acceptance=field(form,"CRITÉRIOS DE APROVAÇÃO","Ex.: sem erros, animação natural e boa performance",true);
        EditText[] editors={project,engine,title,description,acceptance};
        for(int i=0;i<editors.length;i++) if(draft[i]!=null)editors[i].setText(draft[i]);
        Button send=button(submitting?"Enviando...":"Enviar ao estúdio   →",true);
        fullButton(form,send);
        send.setEnabled(!submitting);
        send.setOnClickListener(v->{
            if(submitting)return;
            for(int i=0;i<editors.length;i++)draft[i]=editors[i].getText().toString();
            final String issueTitle,issueBody;
            try{
                issueTitle=TaskFormatter.title(title.getText().toString());
                issueBody=TaskFormatter.body(project.getText().toString(),
                    engine.getText().toString(),description.getText().toString(),
                    acceptance.getText().toString());
            }catch(IllegalArgumentException e){showError(e.getMessage());return;}
            new AlertDialog.Builder(this)
                .setTitle("Enviar solicitação?")
                .setMessage("O texto será publicado como uma Issue no repositório público do GameDev AI Studio. Não envie segredos ou arquivos confidenciais.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Publicar",(d,w)->submit(issueTitle,issueBody,send))
                .show();
        });
        LinearLayout info=card(body);
        add(info,text("🔒  Privacidade",14,WHITE,true));
        gap(info,8);
        add(info,text("Seu token fica criptografado no aparelho. Porém, as tarefas e as respostas publicadas no GitHub são públicas. Nunca inclua senhas, tokens ou dados confidenciais.",12,MUTED,false));
    }

    private void submit(String title,String bodyText,Button send){
        submitting=true;
        send.setEnabled(false);
        send.setText("Enviando...");
        String token=SecureTokenStore.read(this);
        executor.execute(()->{
            try{
                JSONObject created=GitHubService.createTask(token,title,bodyText);
                runOnUiThread(()->{
                    submitting=false;
                    java.util.Arrays.fill(draft,null);
                    detailIssue=created;
                    commentCache=new JSONArray();
                    showNotice("Solicitação publicada com sucesso!");
                    page=Page.DETAIL;
                    render();
                    loadComments();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    submitting=false;
                    send.setEnabled(true);
                    send.setText("Enviar ao estúdio   →");
                    showError(e.getMessage());
                });
            }
        });
    }

    private void renderTasks(){
        heading(body,"Suas solicitações","Tarefas enviadas para o GameDev AI Studio.");
        LinearLayout top=card(body);
        add(top,text(loadingTasks?"Sincronizando com GitHub...":"Atualizadas sob demanda",12,MUTED,false));
        Button reload=button("↻  Atualizar tarefas",false);
        fullButton(top,reload);
        reload.setOnClickListener(v->loadTasks(true));
        if(loadingTasks){
            ProgressBar progress=new ProgressBar(this);
            body.addView(progress);
            gap(body,10);
        }
        if(taskCache.length()==0){
            LinearLayout empty=card(body);
            add(empty,text("Nenhuma solicitação carregada.",15,WHITE,true));
            gap(empty,8);
            add(empty,text("Crie sua primeira tarefa no botão Solicitar ou atualize o histórico.",13,MUTED,false));
            return;
        }
        for(int i=0;i<taskCache.length();i++){
            JSONObject issue=taskCache.optJSONObject(i);
            if(issue==null)continue;
            LinearLayout item=card(body);
            LinearLayout info=row();
            pill(info,"#"+issue.optInt("number"),BLUE);
            LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(0,1,1f);
            info.addView(new View(this),space);
            pill(info,"closed".equals(issue.optString("state"))?"ENCERRADA":"ABERTA",
                "closed".equals(issue.optString("state"))?MUTED:MINT);
            add(item,info);
            gap(item,13);
            add(item,text(issue.optString("title","Tarefa"),16,WHITE,true));
            gap(item,8);
            String date=issue.optString("created_at","");
            if(date.length()>10)date=date.substring(0,10);
            add(item,text(date+"   •   "+issue.optInt("comments")+" comentários",12,MUTED,false));
            Button detail=button("Ver acompanhamento   →",false);
            fullButton(item,detail);
            detail.setOnClickListener(v->{
                detailIssue=issue;
                commentCache=new JSONArray();
                page=Page.DETAIL;
                render();
                loadComments();
            });
        }
    }

    private void loadTasks(boolean announce){
        if(loadingTasks||!hasToken())return;
        loadingTasks=true;
        if(page==Page.TASKS)render();
        String token=SecureTokenStore.read(this);
        executor.execute(()->{
            try{
                JSONArray result=GitHubService.tasks(token);
                runOnUiThread(()->{
                    loadingTasks=false;
                    taskCache=result;
                    if(page==Page.TASKS||page==Page.HOME)render();
                    if(announce)showNotice("Histórico atualizado: "+result.length()+" solicitações.");
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    loadingTasks=false;
                    if(page==Page.TASKS)render();
                    showError(e.getMessage());
                });
            }
        });
    }

    private void renderDetail(){
        if(detailIssue==null){navigate(Page.TASKS);return;}
        Button back=button("←  Voltar às tarefas",false);
        fullButton(body,back);
        back.setOnClickListener(v->navigate(Page.TASKS));
        gap(body,18);
        heading(body,detailIssue.optString("title","Tarefa"),
            "Solicitação #"+detailIssue.optInt("number")+"  •  "+detailIssue.optString("state","open"));
        LinearLayout req=card(body);
        add(req,text("PEDIDO ORIGINAL",11,GOLD,true));
        gap(req,10);
        add(req,text(detailIssue.optString("body",""),13,MUTED,false));
        gap(body,4);
        add(body,text("Acompanhamento do estúdio",18,WHITE,true));
        gap(body,11);
        LinearLayout sync=card(body);
        add(sync,text(loadingComments?"Consultando respostas...":
            "Respostas recebidas: "+commentCache.length(),12,MUTED,false));
        Button reload=button("↻  Verificar novas respostas",false);
        fullButton(sync,reload);
        reload.setOnClickListener(v->loadComments());
        if(loadingComments){
            body.addView(new ProgressBar(this));
            gap(body,10);
        }
        if(commentCache.length()==0){
            LinearLayout wait=card(body);
            add(wait,text("Aguardando resposta do workflow",15,WHITE,true));
            gap(wait,8);
            add(wait,text("A triagem pode levar alguns instantes. Use Verificar novas respostas. Com API não configurada, o workflow fará somente a classificação do especialista.",13,MUTED,false));
        }else{
            for(int i=0;i<commentCache.length();i++){
                JSONObject message=commentCache.optJSONObject(i);
                if(message==null)continue;
                LinearLayout item=card(body);
                JSONObject author=message.optJSONObject("user");
                String name=author==null?"Participante":author.optString("login","Participante");
                add(item,text("✦  "+name,13,MINT,true));
                gap(item,10);
                add(item,text(message.optString("body",""),13,WHITE,false));
            }
        }
    }

    private void loadComments(){
        if(detailIssue==null||loadingComments||!hasToken())return;
        loadingComments=true;
        if(page==Page.DETAIL)render();
        int number=detailIssue.optInt("number");
        String token=SecureTokenStore.read(this);
        executor.execute(()->{
            try{
                JSONArray result=GitHubService.comments(token,number);
                runOnUiThread(()->{
                    loadingComments=false;
                    if(detailIssue!=null&&detailIssue.optInt("number")==number){
                        commentCache=result;
                        if(page==Page.DETAIL)render();
                    }
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    loadingComments=false;
                    if(page==Page.DETAIL)render();
                    showError(e.getMessage());
                });
            }
        });
    }

    private void renderSettings(){
        heading(body,"Configurações","Conecte sua conta uma única vez. Depois, use o aplicativo normalmente.");
        LinearLayout account=card(body);
        add(account,text("CONTA GITHUB",11,GOLD,true));
        gap(account,10);
        add(account,text(hasToken()?"● Credencial salva com criptografia":"○ Conta ainda não conectada",
            15,hasToken()?MINT:MUTED,true));
        gap(account,9);
        add(account,text("Repositório central: "+GitHubService.REPO,12,MUTED,false));
        gap(account,16);

        add(account,text("1  Criar um token com acesso restrito",14,WHITE,true));
        gap(account,7);
        add(account,text("No GitHub, escolha Fine-grained token. Selecione somente o repositório GameDevAiStudio e conceda Repository permissions > Issues: Read and write. Token com acesso somente a Metadata não pode criar tarefas.",12,MUTED,false));
        Button getToken=button("Abrir criação de token no GitHub  ↗",false);
        fullButton(account,getToken);
        getToken.setOnClickListener(v->openLink("https://github.com/settings/personal-access-tokens/new"));
        gap(account,22);

        add(account,text("2  Cole o token aqui",14,WHITE,true));
        gap(account,9);
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTextSize(15);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        input.setHint("github_pat_...");
        input.setPadding(dp(15),dp(13),dp(15),dp(13));
        input.setBackground(background(Color.rgb(12,22,40),12,BORDER));
        account.addView(input,new LinearLayout.LayoutParams(-1,-2));
        gap(account,8);
        add(account,text("A credencial é criptografada com Android Keystore e não é incluída nos backups.",12,MUTED,false));
        Button save=button("Validar e conectar",true);
        fullButton(account,save);
        save.setOnClickListener(v->{
            String candidate=input.getText().toString().trim();
            if(candidate.length()<20){showError("Cole o token Fine-grained criado para este repositório.");return;}
            save.setText("Verificando...");
            save.setEnabled(false);
            executor.execute(()->{
                try{
                    JSONObject profile=GitHubService.identity(candidate);
                    String login=profile.optString("login","");
                    if(!GitHubService.OWNER.equalsIgnoreCase(login))
                        throw new IllegalStateException("O token pertence a @"+login+
                            ". Conecte a conta @"+GitHubService.OWNER+", proprietária do repositório.");
                    GitHubService.verifyIssueRead(candidate);
                    SecureTokenStore.save(this,candidate);
                    runOnUiThread(()->{
                        showNotice("Conta GitHub conectada.");
                        page=Page.HOME;
                        render();
                        loadTasks(false);
                    });
                }catch(Exception e){
                    runOnUiThread(()->{
                        save.setEnabled(true);
                        save.setText("Validar e conectar");
                        showError(e.getMessage());
                    });
                }
            });
        });
        if(hasToken()){
            gap(account,12);
            Button delete=button("Desconectar e apagar token salvo",false);
            fullButton(account,delete);
            delete.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Desconectar GitHub?")
                .setMessage("O token salvo neste aparelho será apagado. Suas tarefas continuam no GitHub.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Desconectar",(d,w)->{
                    SecureTokenStore.clear(this);
                    taskCache=new JSONArray();
                    showNotice("Credencial removida.");
                    render();
                }).show());
        }
        LinearLayout note=card(body);
        add(note,text("Você só precisa entrar no GitHub nesta configuração inicial.",14,WHITE,true));
        gap(note,8);
        add(note,text("Depois, tudo acontece no GameDev AI Studio: solicitar, acompanhar e consultar respostas. A automação dos agentes usa GitHub Actions; IA generativa exige uma configuração adicional no repositório.",12,MUTED,false));
        gap(note,10);
        add(note,text("Versão de testes 0.2.1 • Diagnóstico de API e permissões • Sem coleta de dados analíticos.",11,GOLD,false));
    }
    private void openLink(String url){
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }
    private void showError(String message){
        final String detail=message==null?"Erro inesperado.":message;
        new AlertDialog.Builder(this).setTitle("Diagnóstico de conexão / API")
            .setMessage(detail)
            .setNeutralButton("Copiar diagnóstico",(dialog,which)->{
                android.content.ClipboardManager clip=
                    (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                if(clip!=null){
                    clip.setPrimaryClip(android.content.ClipData.newPlainText("Erro GameDev AI Studio",detail));
                    showNotice("Diagnóstico copiado. Não compartilhe seu token.");
                }
            })
            .setPositiveButton("OK",null).show();
    }
    private void showNotice(String value){
        Toast.makeText(this,value,Toast.LENGTH_LONG).show();
    }
}
