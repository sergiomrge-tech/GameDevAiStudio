package com.gamedev.aistudio;
public final class TaskFormatter {
 private TaskFormatter(){}
 public static String title(String input) {
  String cleaned=require(input,"Título",3,155);
  while(cleaned.startsWith("[JOGO]")) cleaned=cleaned.substring(6).trim();
  return "[JOGO] "+require(cleaned,"Título",3,155);
 }
 public static String body(String project,String engine,String description,String acceptance){
  String name=require(project,"Projeto",2,120);
  String platform=require(engine,"Engine",2,60);
  String req=require(description,"Descrição",10,7000);
  String criteria=acceptance==null?"":acceptance.trim();
  if(criteria.length()>2500) throw new IllegalArgumentException("Critérios excedem 2500 caracteres");
  return "## Tarefa enviada pelo GameDev AI Studio Android\n\n"
   +"**Projeto:** "+name+"\n\n**Engine:** "+platform+"\n\n"
   +"### Solicitação\n"+req+"\n\n### Critérios de aprovação\n"
   +(criteria.isEmpty()?"A definir após análise.":criteria)
   +"\n\n---\nOrigem: aplicativo Android v0.2.0. Esta Issue solicita triagem e planejamento, não significa que a alteração foi executada.";
 }
 static String require(String s,String label,int min,int max){
  String r=s==null?"":s.trim();
  if(r.length()<min||r.length()>max) throw new IllegalArgumentException(label+" deve ter entre "+min+" e "+max+" caracteres.");
  return r;
 }
}