package vn.edu.usth.camerasystem.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import vn.edu.usth.camerasystem.R;
import vn.edu.usth.camerasystem.storage.EventRepository;
import vn.edu.usth.camerasystem.storage.SecurityEvent;
import vn.edu.usth.camerasystem.ui.AppController;

/**
 * Events tab — đọc EventRepository. P5 thay TextView bằng RecyclerView.
 */
public class EventsFragment extends Fragment implements EventRepository.Observer {

    private TextView eventsListText;
    private LinearLayout emptyState;
    private EventRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsListText = view.findViewById(R.id.events_list_text);
        emptyState = view.findViewById(R.id.events_empty_state);
        repository = AppController.get().getEventRepository();
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.setObserver(this);
    }

    @Override
    public void onPause() {
        repository.setObserver(null);
        super.onPause();
    }

    @Override
    public void onEventsChanged(@NonNull List<SecurityEvent> events) {
        if (!isAdded()) {
            return;
        }
        if (events.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            eventsListText.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        eventsListText.setVisibility(View.VISIBLE);

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        StringBuilder builder = new StringBuilder();
        for (SecurityEvent event : events) {
            builder.append("• ")
                    .append(format.format(new Date(event.getTimestampMs())));
            if (event.getNote() != null) {
                builder.append('\n')
                        .append("  ")
                        .append(event.getNote());
            }
            if (event.getVideoPath() != null) {
                builder.append('\n')
                        .append("  ")
                        .append(event.getVideoPath());
            }
            builder.append("\n\n");
        }
        eventsListText.setText(builder.toString().trim());
    }
}
