package com.emmaguy.videopocket.video;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
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

class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    private static final int SECONDS_IN_A_MINUTE = 60;

    private final List<Video> mVideos = new ArrayList<>();

    @Override public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_video, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Resources resources = holder.mTitle.getResources();

        final Video video = mVideos.get(position);
        holder.mTitle.setText(resources.getString(R.string.row_title_format, position + 1, video.getTitle()));

        final Duration duration = video.getDuration();

        final long durationInMinutes = duration.getSeconds() / (SECONDS_IN_A_MINUTE * SECONDS_IN_A_MINUTE);
        final long durationInSeconds = (duration.getSeconds() % (SECONDS_IN_A_MINUTE * SECONDS_IN_A_MINUTE)) / SECONDS_IN_A_MINUTE;
        holder.mDuration.setText(String.format("%d:%02d", durationInMinutes, durationInSeconds));
    }

    @Override public int getItemCount() {
        return mVideos.size();
    }

    public void updateVideos(List<Video> videos) {
        mVideos.clear();
        mVideos.addAll(videos);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.videos_title) TextView mTitle;
        @Bind(R.id.videos_duration) TextView mDuration;
        @Bind(R.id.videos_open_pocket) TextView mOpenPocket;
        @Bind(R.id.videos_video_container) View mContainer;

        public ViewHolder(final View view) {
            super(view);

            ButterKnife.bind(this, view);

            mContainer.setOnClickListener(v -> view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mVideos.get(getAdapterPosition()).getUrl()))));
            mOpenPocket.setOnClickListener(v -> view.getContext().startActivity(PocketUtils.sendUrlToPocket(view.getContext(), mVideos.get(getAdapterPosition()).getUrl())));
        }
    }
}
