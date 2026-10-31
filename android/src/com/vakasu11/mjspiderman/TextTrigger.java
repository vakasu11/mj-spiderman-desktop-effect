package com.vakasu11.mjspiderman;

/** Examines only the two characters immediately before the changed cursor. */
public final class TextTrigger {
    private long lastEvent = -1;
    private int lastWindow = -1;
    private int lastCursor = -1;

    public void reset() { lastEvent = -1; lastWindow = -1; lastCursor = -1; }

    public boolean matches(CharSequence text, int from, int added, boolean password,
                           boolean editable, int window, long eventTime) {
        if (password || !editable || text == null || added <= 0 || from < 0) return false;
        long endLong = (long) from + added;
        if (endLong < 2 || endLong > text.length()) return false;
        int end = (int) endLong;
        if (Character.toLowerCase(text.charAt(end - 2)) != 'm'
                || Character.toLowerCase(text.charAt(end - 1)) != 'j') return false;
        if (eventTime == lastEvent && window == lastWindow && end == lastCursor) return false;
        lastEvent = eventTime;
        lastWindow = window;
        lastCursor = end;
        return true;
    }
}
