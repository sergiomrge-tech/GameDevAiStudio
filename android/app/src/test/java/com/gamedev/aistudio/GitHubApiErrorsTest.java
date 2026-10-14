package com.gamedev.aistudio;
import org.junit.Test;
import static org.junit.Assert.*;
public class GitHubApiErrorsTest {
 @Test public void permissionErrorHasFixInstructions(){
  String m=GitHubApiErrors.describe(403,"Resource not accessible by personal access token","53");
  assertTrue(m.contains("Issues → Read and write"));
  assertTrue(m.contains("GameDevAiStudio"));
  assertTrue(m.contains("HTTP 403"));
 }
 @Test public void rateLimitIsDistinctFromPermissions(){
  String m=GitHubApiErrors.describe(403,"API rate limit exceeded","0");
  assertTrue(m.contains("Limite"));
  assertFalse(m.contains("Read and write"));
 }
 @Test public void authAndMissingRepoAreDistinct(){
  assertTrue(GitHubApiErrors.describe(401,"","").contains("expirado"));
  assertTrue(GitHubApiErrors.describe(404,"","").contains("Repositório"));
 }
 @Test public void githubMessageIsTruncated(){
  String m=GitHubApiErrors.describe(422,"x".repeat(900),"100");
  assertTrue(m.length()<400);
 }
 @Test public void networkShowsReadableErrors(){
  assertTrue(GitHubApiErrors.network(new java.net.UnknownHostException("api.github.com")).contains("DNS"));
  assertTrue(GitHubApiErrors.network(new java.net.SocketTimeoutException()).contains("Tempo"));
 }
}
