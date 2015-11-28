package com.emmaguy.videopocket.feature.video;

import java.util.List;

class YouTubeVideoResponse {
    private final List<YouTubeResponse> items;

    YouTubeVideoResponse(List<YouTubeResponse> items) {
        this.items = items;
    }

    List<YouTubeResponse> getItems() {
        return items;
    }

    static class YouTubeResponse {
        private final Statistics statistics;
        private final ContentDetails contentDetails;
        private final String id;

        YouTubeResponse(ContentDetails contentDetails, Statistics statistics, String id) {
            this.contentDetails = contentDetails;
            this.id = id;
            this.statistics = statistics;
        }

        ContentDetails getContentDetails() {
            return contentDetails;
        }

        Statistics getStatistics() {
            return statistics;
        }

        String getId() {
            return id;
        }

        static class ContentDetails {
            private final String duration;

            ContentDetails(String duration) {
                this.duration = duration;
            }

            String getDuration() {
                return duration;
            }
        }

        class Statistics {
            private final long viewCount;

            Statistics(final long viewCount) {
                this.viewCount = viewCount;
            }

            long getViewCount() {
                return viewCount;
            }
        }
    }
}
