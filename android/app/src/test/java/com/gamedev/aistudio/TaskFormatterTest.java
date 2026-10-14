package com.gamedev.aistudio;
import org.junit.Test;
import static org.junit.Assert.*;
public class TaskFormatterTest{
 @Test public void titlePrefixIsNotRepeated(){
  assertEquals("[JOGO] Ajustar personagem",TaskFormatter.title("Ajustar personagem"));
  assertEquals("[JOGO] Ajustar personagem",TaskFormatter.title("[JOGO] Ajustar personagem"));
 }
 @Test public void fieldsAreIncluded(){
  String b=TaskFormatter.body("Dungeons Fire","Godot","Melhorar animacao de corrida","60 FPS");
  assertTrue(b.contains("Dungeons Fire"));assertTrue(b.contains("Godot"));
  assertTrue(b.contains("60 FPS"));assertTrue(b.contains("não significa"));
 }
 @Test(expected=IllegalArgumentException.class) public void tooShort(){TaskFormatter.title("x");}
 @Test(expected=IllegalArgumentException.class) public void emptyDescription(){TaskFormatter.body("Jogo","Godot","curto","");}
}