package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class ContinueRulesTest {
 @Test public void alternatesSources(){assertEquals(java.util.Arrays.asList("ivi-a","lmd-a","ivi-b","ivi-c"), ContinueRules.balance(java.util.Arrays.asList("ivi-a","ivi-b","ivi-c","lmd-a"), item -> item.substring(0,3)));}
 @Test public void keepsIncomplete(){assertTrue(ContinueRules.include(0,100,1000,"Фильм","app://film"));}
 @Test public void hidesFinished(){assertFalse(ContinueRules.include(0,990,1000,"Фильм","app://film"));}
 @Test public void excludesPromotions(){assertFalse(ContinueRules.include(2,0,1000,"Реклама","app://film"));}
 @Test public void nextEpisodeNeedsNoPosition(){assertTrue(ContinueRules.include(1,0,0,"Серия","app://next"));}
 @Test public void rejectsMissingLink(){assertFalse(ContinueRules.include(0,1,100,"Фильм"," "));}
 @Test public void rejectsMissingTitle(){assertFalse(ContinueRules.include(0,1,100,null,"app://film"));}
 @Test public void avoidsOverflow(){assertEquals(50,ContinueRules.percent(Long.MAX_VALUE/2,Long.MAX_VALUE));}
 @Test public void clampsInvalidProgress(){assertEquals(0,ContinueRules.percent(-1,100));assertEquals(100,ContinueRules.percent(200,100));}
}
