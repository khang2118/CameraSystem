package vn.edu.usth.camerasystem.ui.events;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.storage.SecurityEvent;

/** Presents security events; does not access the database directly. */
public final class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventHolder> {

    public interface Listener {
        void onPlay(@NonNull SecurityEvent event);
        void onDelete(@NonNull SecurityEvent event);
    }

    private final List<SecurityEvent> events = new ArrayList<>();
    private final Listener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT);

    public EventAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitEvents(@NonNull List<SecurityEvent> updated) {
        events.clear();
        events.addAll(updated);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        root.setBackgroundColor(0xFFF5F7FA);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
        root.setLayoutParams(params);

        TextView title = text(context, 16, true);
        TextView note = text(context, 14, false);
        TextView path = text(context, 12, false);
        path.setTextColor(0xFF616A75);
        root.addView(title);
        root.addView(note);
        root.addView(path);

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.END);
        Button play = new Button(context);
        play.setText(R.string.events_play);
        Button delete = new Button(context);
        delete.setText(R.string.events_delete);
        actions.addView(play);
        actions.addView(delete);
        root.addView(actions);
        return new EventHolder(root, title, note, path, play, delete);
    }

    @Override
    public void onBindViewHolder(@NonNull EventHolder holder, int position) {
        SecurityEvent event = events.get(position);
        holder.title.setText(dateFormat.format(new Date(event.getTimestampMs())));
        String note = event.getNote();
        holder.note.setText(note == null || note.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.events_default_note) : note);
        String videoPath = event.getVideoPath();
        boolean hasVideo = videoPath != null && !videoPath.trim().isEmpty();
        holder.path.setText(hasVideo
                ? holder.itemView.getContext().getString(R.string.events_video_path, videoPath)
                : holder.itemView.getContext().getString(R.string.events_no_attachment));
        holder.play.setEnabled(hasVideo);
        holder.play.setOnClickListener(v -> listener.onPlay(event));
        holder.delete.setOnClickListener(v -> listener.onDelete(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private static TextView text(Context context, float size, boolean bold) {
        TextView view = new TextView(context);
        view.setTextSize(size);
        view.setTextColor(0xFF20242A);
        view.setPadding(0, dp(context, 4), 0, dp(context, 4));
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static final class EventHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView note;
        final TextView path;
        final Button play;
        final Button delete;

        EventHolder(@NonNull View itemView, TextView title, TextView note,
                    TextView path, Button play, Button delete) {
            super(itemView);
            this.title = title;
            this.note = note;
            this.path = path;
            this.play = play;
            this.delete = delete;
        }
    }
}
