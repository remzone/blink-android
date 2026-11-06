package com.gathin.tvhome;

/** Правила отбора реального продолжения просмотра без рекламных рекомендаций. */
final class ContinueRules {
    static boolean include(int type, long position, long duration, String title, String link) {
        return (type == 0 || type == 1) && title != null && !title.trim().isEmpty()
                && link != null && !link.trim().isEmpty()
                && (duration <= 0 || position <= 0 || (double) position / duration < .98);
    }
    static <T> java.util.List<T> balance(java.util.List<T> entries, java.util.function.Function<T, String> source) {
        java.util.Map<String, java.util.ArrayDeque<T>> groups = new java.util.LinkedHashMap<>();
        for (T entry : entries) groups.computeIfAbsent(source.apply(entry), key -> new java.util.ArrayDeque<>()).add(entry);
        java.util.List<T> result = new java.util.ArrayList<>(); boolean more;
        do { more = false; for (java.util.ArrayDeque<T> group : groups.values()) if (!group.isEmpty()) { result.add(group.removeFirst()); more = true; } } while (more);
        return result;
    }
    static int percent(long position, long duration) {
        return duration <= 0 ? 0 : (int) Math.max(0, Math.min(100, 100.0 * position / duration));
    }
}
