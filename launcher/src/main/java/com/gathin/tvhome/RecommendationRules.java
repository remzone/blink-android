package com.gathin.tvhome;
import java.util.Locale;
/** История, избранное и список просмотра остаются вне рекомендаций. */
final class RecommendationRules {
    static boolean channel(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String value = name.toLowerCase(Locale.ROOT);
        return !value.contains("истори") && !value.contains("избран")
                && !value.contains("history") && !value.contains("favorite")
                && !value.contains("watchlist") && !value.contains("watch next")
                && !value.contains("продолжить") && !value.contains("смотреть позже");
    }
    static boolean program(String title, String link) {
        return title != null && !title.trim().isEmpty() && link != null && !link.trim().isEmpty();
    }
}
