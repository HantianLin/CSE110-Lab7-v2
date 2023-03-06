package edu.ucsd.cse110.sharednotes.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class NoteRepository {
    private final NoteDao dao;

    private final NoteAPI api;
    private ScheduledFuture<?> noteFuture;
    private final MutableLiveData<Note> realNoteData;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    public NoteRepository(NoteDao dao,NoteAPI api) {
        this.dao = dao;
        this.api = api;
        realNoteData = new MutableLiveData<>();
    }


    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     *
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        var note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = theirNote -> {
            var ourNote = note.getValue();
            if (theirNote == null) return; // do nothing
            if (ourNote == null || ourNote.updatedAt < theirNote.updatedAt) {
                upsertLocal(theirNote);
            }
        };

        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();
    }

    public void upsertLocal(Note note) {
        note.updatedAt = Instant.now().getEpochSecond();
        dao.upsert(note);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // TODO: Implement getRemote!
        // TODO: Set up polling background thread (MutableLiveData?)
        // TODO: Refer to TimerService from https://github.com/DylanLukes/CSE-110-WI23-Demo5-V2.
        if (noteFuture != null && !noteFuture.isCancelled()) {
            noteFuture.cancel(true);
        }
        var executor = Executors.newSingleThreadScheduledExecutor();
        noteFuture = executor.scheduleAtFixedRate(() -> {
            var note = api.getNote(title);
            realNoteData.postValue(note);
        }, 0, 3000, TimeUnit.MILLISECONDS);
        // Start by fetching the note from the server _once_ and feeding it into MutableLiveData.
        // Then, set up a background thread that will poll the server every 3 seconds.

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.
        // You don't need to worry about killing background threads.

        return realNoteData;
    }

    public void upsertRemote(Note note) {
        // TODO: Implement upsertRemote!
        var executor = Executors.newSingleThreadScheduledExecutor();
        noteFuture = executor.scheduleAtFixedRate(() -> {
            JsonObject json = new JsonObject();
            json.addProperty("content",note.content);
            json.addProperty("updated_at",note.updatedAt);

            api.putNote(note.title, RequestBody.create(JSON,json.toString()));
            realNoteData.postValue(note);
        }, 0, 3000, TimeUnit.MILLISECONDS);    }
}
