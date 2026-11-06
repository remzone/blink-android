package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
public class CaptchaPolicyTest {
 @Test public void permitsVKChallengeOriginsOnly()throws Exception{
  assertEquals("https://api.vk.ru",CaptchaPolicy.origin("https://api.vk.ru/captcha?session_token=fixture"));
  assertEquals("https://id.vk.com",CaptchaPolicy.origin("https://id.vk.com/captcha"));
  for(String u:new String[]{"http://vk.ru/x","https://vk.ru.evil.example/x","https://evilvk.ru/x","https://user@vk.ru/x","https://vk.ru:444/x","https://vk.ru/x#fragment","javascript:alert(1)","https://127.0.0.1/x"})try{CaptchaPolicy.origin(u);fail(u);}catch(Exception expected){}
 }
 @Test public void stacksPhoneCardsAndKeepsWideScreensInColumns(){assertFalse(DeviceLayout.twoColumns(360));assertFalse(DeviceLayout.twoColumns(640));assertTrue(DeviceLayout.twoColumns(700));assertTrue(DeviceLayout.twoColumns(960));}
}
