package com.onlinepaste.domain;

/**
 * A paste together with its current view counter.
 */
public record PasteWithViews(Paste paste, long views) {
}
