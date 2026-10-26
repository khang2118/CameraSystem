package vn.edu.usth.camerasystem.ui.events;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;
import vn.edu.usth.camerasystem.ui.AppController;

/** P5 Events screen: Room-backed list, in-app playback and event deletion. */
public final class EventsFragment extends Fragment implements EventRepository.Observer,
        EventAdapter.Listener {

    @Nullable private EventRepository repository;
    @Nullable private RecyclerView recyclerView;
    @Nullable private View emptyView;
    @Nullable private EventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView list = view.findViewById(R.id.events_list);
        EventAdapter eventAdapter = new EventAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(eventAdapter);

        recyclerView = list;
        emptyView = view.findViewById(R.id.events_empty_state);
        adapter = eventAdapter;
        repository = AppController.get().getEventRepository();
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
            Toast.makeText(requireContext(), R.string.events_no_video, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri;
        if (path.startsWith("content://")) {
            uri = Uri.parse(path);
        } else {
            File file = new File(path);
            if (!file.isFile()) {
                Toast.makeText(requireContext(), R.string.events_file_missing, Toast.LENGTH_LONG).show();
                return;
            }
            uri = Uri.fromFile(file);
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
                .setTitle(R.string.events_video_title)
                .setView(wrapper)
                .setPositiveButton(R.string.events_close, (d, which) -> d.dismiss())
                .create();
        dialog.setOnShowListener(d -> video.start());
        dialog.setOnDismissListener(d -> video.stopPlayback());
        video.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(context, R.string.events_play_error, Toast.LENGTH_SHORT).show();
            return true;
        });
        dialog.show();
    }

    @Override
    public void onDelete(@NonNull SecurityEvent event) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.events_delete_title)
                .setMessage(R.string.events_delete_message)
                .setNegativeButton(R.string.events_cancel, null)
                .setPositiveButton(R.string.events_delete, (dialog, which) -> {
                    if (repository == null) return;
                    boolean accepted = repository.deleteEvent(event.getId());
                    Toast.makeText(requireContext(), accepted
                            ? R.string.events_deleted : R.string.events_not_found, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
