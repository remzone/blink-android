package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecommendationRulesTest {
 @Test public void keepsEditorialChannels() { assertTrue(RecommendationRules.channel("Горячие новинки")); assertTrue(RecommendationRules.channel("Популярные детективы на Иви")); }
 @Test public void excludesPersonalLists() { for (String name : new String[]{"LazyMedia Deluxe • История", "Избранное", "Watchlist", "History", "Смотреть позже", "Продолжить просмотр"}) assertFalse(name, RecommendationRules.channel(name)); }
 @Test public void requiresUsableCard() { assertFalse(RecommendationRules.program(null, "app://film")); assertFalse(RecommendationRules.program("Фильм", " ")); assertTrue(RecommendationRules.program("Фильм", "app://film")); }
}
