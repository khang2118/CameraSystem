package vn.edu.usth.camerasystem.ui.events;

import android.view.Gravity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;
import vn.edu.usth.camerasystem.ui.AppController;

/** P5 Events screen: Room-backed list, in-app playback and event deletion. */
public final class EventsFragment extends Fragment implements EventRepository.Observer,
        EventAdapter.Listener {

    @Nullable private EventRepository repository;
    @Nullable private RecyclerView recyclerView;
    @Nullable private TextView emptyView;
    @Nullable private EventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle state) {
        Context context = requireContext();
        FrameLayout root = new FrameLayout(context);

        RecyclerView list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        EventAdapter eventAdapter = new EventAdapter(this);
        list.setAdapter(eventAdapter);
        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView empty = new TextView(context);
        empty.setText("Chua co su kien camera");
        empty.setTextSize(17);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        recyclerView = list;
        emptyView = empty;
        adapter = eventAdapter;
        repository = AppController.get().getEventRepository();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) repository.setObserver(this);
    }

    @Override
    public void onPause() {
        if (repository != null) repository.setObserver(null);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (recyclerView != null) recyclerView.setAdapter(null);
        recyclerView = null;
        emptyView = null;
        adapter = null;
        repository = null;
        super.onDestroyView();
    }

    @Override
    public void onEventsChanged(@NonNull List<SecurityEvent> events) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> render(events));
        } else {
            render(events);
        }
    }

    private void render(@NonNull List<SecurityEvent> events) {
        if (!isAdded() || getView() == null || adapter == null || emptyView == null || recyclerView == null) {
            return;
        }
        adapter.submitEvents(events);
        boolean empty = events.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPlay(@NonNull SecurityEvent event) {
        if (!isAdded()) return;
        String path = event.getVideoPath();
        if (path == null || path.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Su kien khong co video", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri;
        if (path.startsWith("content://")) {
            uri = Uri.parse(path);
        } else {
            File file = new File(path);
            if (!file.isFile()) {
                Toast.makeText(requireContext(), "Khong tim thay file video", Toast.LENGTH_LONG).show();
                return;
            }
            uri = Uri.fromFile(file); // Only used inside our own VideoView; not shared with another app.
        }
        Context context = requireContext();
        VideoView video = new VideoView(context);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        int height = (int) (260 * context.getResources().getDisplayMetrics().density);
        wrapper.addView(video, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));
        MediaController controls = new MediaController(context);
        video.setMediaController(controls);
        video.setVideoURI(uri);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Video su kien")
                .setView(wrapper)
                .setPositiveButton("Dong", (d, which) -> d.dismiss())
                .create();
        dialog.setOnShowListener(d -> video.start());
        dialog.setOnDismissListener(d -> video.stopPlayback());
        video.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(context, "Khong phat duoc video", Toast.LENGTH_SHORT).show();
            return true;
        });
        dialog.show();
    }

    @Override
    public void onDelete(@NonNull SecurityEvent event) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoa su kien?")
                .setMessage("Xoa su kien khoi lich su? Video trong vung luu tru cua ung dung cung se bi xoa neu co.")
                .setNegativeButton("Huy", null)
                .setPositiveButton("Xoa", (dialog, which) -> {
                    if (repository == null) return;
                    boolean accepted = repository.deleteEvent(event.getId());
                    Toast.makeText(requireContext(), accepted
                            ? "Da gui yeu cau xoa" : "Khong tim thay su kien", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
