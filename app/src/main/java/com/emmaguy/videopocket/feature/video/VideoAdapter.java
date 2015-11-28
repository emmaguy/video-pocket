package com.emmaguy.videopocket.feature.video;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.emmaguy.videopocket.R;

import org.threeten.bp.Duration;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    private static final int SECONDS_IN_A_MINUTE = 60;

    private final List<Video> videos = new ArrayList<>();

    @Override public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_video, parent, false);
        return new ViewHolder(view, videos);
    }

    @Override public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Video video = videos.get(position);
        holder.setVideo(video);
    }

    @Override public int getItemCount() {
        return videos.size();
    }

    void updateVideos(@NonNull final List<Video> videos) {
        this.videos.clear();
        this.videos.addAll(videos);
        notifyDataSetChanged();
    }

    void removeVideo(final Video video) {
        final int index = videos.indexOf(video);
        videos.remove(index);
        notifyItemRemoved(index);

        // update the numbering
        notifyDataSetChanged();
    }

    Video getItemAt(final int position) {
        return videos.get(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final List<Video> videos;

        @Bind(R.id.video_title) TextView title;
        @Bind(R.id.video_duration) TextView duration;
        @Bind(R.id.video_view_count) TextView viewCount;

        ViewHolder(@NonNull final View view, @NonNull final List<Video> videos) {
            super(view);
            this.videos = videos;

            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.videos_video_item_container) void onViewClicked() {
            final Video video = videos.get(getAdapterPosition());
            itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(video.getUrl())));
        }

        private void setVideo(@NonNull final Video video) {
            title.setText(video.getTitle());
            viewCount.setText(title.getResources().getString(R.string.views_format, video.getViewCount()));

            final Duration dur = video.getDuration();
            final long durationInMinutes = dur.getSeconds() / (SECONDS_IN_A_MINUTE * SECONDS_IN_A_MINUTE);
            final long durationInSeconds = (dur.getSeconds() % (SECONDS_IN_A_MINUTE * SECONDS_IN_A_MINUTE)) / SECONDS_IN_A_MINUTE;
            duration.setText(String.format("%d:%02d", durationInMinutes, durationInSeconds));
        }
    }
}
