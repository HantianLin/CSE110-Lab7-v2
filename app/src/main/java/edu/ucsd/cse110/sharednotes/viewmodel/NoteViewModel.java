package edu.ucsd.cse110.sharednotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import edu.ucsd.cse110.sharednotes.model.Note;
import edu.ucsd.cse110.sharednotes.model.NoteAPI;
import edu.ucsd.cse110.sharednotes.model.NoteDatabase;
import edu.ucsd.cse110.sharednotes.model.NoteRepository;
import okhttp3.MediaType;

public class NoteViewModel extends AndroidViewModel {
    private LiveData<Note> note;
    private final NoteRepository repo;
    private ScheduledFuture<?> noteFuture;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        var context = application.getApplicationContext();
        var db = NoteDatabase.provide(context);
        var dao = db.getDao();
        NoteAPI api  = NoteAPI.provide(); //added

        this.repo = new NoteRepository(dao,api);
    }

    public LiveData<Note> getNote(String title) {
        // TODO: use getSynced here instead?
        // The returned live data should update whenever there is a change in
        // the database, or when the server returns a newer version of the note.
        // Polling interval: 3s.
//        if(noteFuture != null && !noteFuture.isCancelled()){
//            noteFuture.cancel(true);
//        }
//        var executor = Executors.newSingleThreadScheduledExecutor();
//        noteFuture = executor.scheduleAtFixedRate(() -> {
//            note = repo.getSynced(title) ;
//        }, 0, 3000, TimeUnit.MILLISECONDS);
//        note = repo.getSynced(title);//added
        if (note == null) {
            note = repo.getSynced(title);
        }
        return note;
    }

    public void save(Note note) {
        // TODO: try to upload the note to the server.
        repo.upsertRemote(note); //added
        repo.upsertLocal(note);
    }
}
