package com.emmaguy.videopocket.feature.video;

public enum SortOrder {
    VIDEO_DURATION(0),
    TIME_ADDED_TO_POCKET(1);

    private final int index;

    SortOrder(final int index) {
        this.index = index;
    }

    public static SortOrder fromIndex(final int index) {
        return index == VIDEO_DURATION.index ? SortOrder.VIDEO_DURATION : SortOrder.TIME_ADDED_TO_POCKET;
    }

    public int getIndex() {
        return index;
    }
}
