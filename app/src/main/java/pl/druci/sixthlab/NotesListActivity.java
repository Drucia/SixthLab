package pl.druci.sixthlab;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class NotesListActivity extends AppCompatActivity
{
    private PlayAudio playTask;
    private boolean isPlaying = false;
    private int current_note;
    private int to_merge;
    private boolean b_is_contex = false;
    private boolean b_is_merge = false;
    private Note notes[];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_notes);

        vUpdateNotes();
        ListView list_notes = (ListView) findViewById(R.id.listView);

        registerForContextMenu(list_notes);
        list_notes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                if (isPlaying)
                    stopPlaying();

                current_note = (int) id;

                if (!b_is_contex && !b_is_merge)
                    play();

                if (b_is_merge)
                    if (to_merge != current_note)
                        vClickedMerge(to_merge, current_note);
                    else
                    {
                        Toast.makeText(NotesListActivity.this, "Can't mix the same note", Toast.LENGTH_SHORT).show();
                        b_is_merge = false;
                        b_is_contex = false;
                    } // (to_merge == current_note)
            } // public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        });

        ArrayAdapter<Note> list_adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notes);
        list_notes.setAdapter(list_adapter);
    } // protected void onCreate(Bundle savedInstanceState)

    private void vUpdateNotes()
    {
        File main_dir = new File(MainActivity.path.getAbsolutePath());
        String file_list[] = main_dir.list();
        notes = new Note[file_list.length/2];
        int ii = 0;

        for (String f : file_list)
        {
            if (f.endsWith(".txt"))
            {
                notes[ii] = new Note(MainActivity.path.getAbsolutePath() + "/" + f);
                notes[ii++].vReadFromFile();
            } // if (f.endsWith(".txt"))
        } // for (String f : file_list)
    } // private void vUpdateNotes()

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        if (!b_is_merge && !isPlaying)
        {
            super.onCreateContextMenu(menu, v, menuInfo);

            b_is_contex = true;

            menu.add(Menu.NONE, 0, 1, "Delete");
            menu.add(Menu.NONE, 1, 2, "Merge");
        } // if (!b_is_merge)
    } // public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo adap = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        current_note = adap.position;

        switch (item.getItemId())
        {
            case 0:
                vDeleteFiles(notes[current_note].getSound_path(), notes[current_note].getPath());
                return true;
            case 1:
                to_merge = current_note;
                b_is_merge = true;
                Toast.makeText(this, "Select note to mix", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        } //switch (item.getItemId())
    } // public boolean onContextItemSelected(MenuItem item)

    private void vClickedMerge(int iPos1, int iPos2)
    {
        int bufferSize = AudioTrack.getMinBufferSize(MainActivity.frequency,MainActivity.channelConfiguration, MainActivity.audioEncoding);
        short[] audiodata = new short[bufferSize / 4];

        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(notes[iPos2].getSound_path())));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(notes[iPos1].getSound_path(), true)));

            while (b_is_merge && dis.available() > 0)
            {
                int i = 0;
                while (dis.available() > 0 && i < audiodata.length) {
                    audiodata[i] = dis.readShort();
                    dos.writeShort(audiodata[i]);
                    i++;
                }
            } // while (isPlaying && dis.available() > 0)
            dis.close();
            dos.close();
            b_is_merge = false;
            Note note = notes[iPos2];
            notes[iPos1].vAddMeta(note.getName(), note.getSurname(), note.getTitle(), note.getDescription());
            Toast.makeText(this, "Notes mixted", Toast.LENGTH_SHORT).show();
        } catch (Throwable t)
        {
            Log.e("AudioTrack", "Playback Failed");
        } // catch (Throwable t)

        vDeleteFiles(notes[iPos2].getSound_path(), notes[iPos2].getPath());
    } // private void vClickedMarge(int iPos1)

    private void vDeleteFiles(File pathPcm, String pathTxt)
    {
        pathPcm.delete();
        new File(pathTxt).delete();
        recreate();
    } // private void vDeleteFiles(File pathPcm, String pathTxt)

    @Override
    protected void onPause()
    {
        super.onPause();
        stopPlaying();
        b_is_merge = false;
    } // protected void onPause()

    public void play()
    {
        playTask = new PlayAudio();
        playTask.execute();
    } // public void play()

    public void stopPlaying()
    {
        isPlaying = false;
    } // public void stopPlaying()

    private class PlayAudio extends AsyncTask<Void, Integer, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            isPlaying = true;

            int bufferSize = AudioTrack.getMinBufferSize(MainActivity.frequency,MainActivity.channelConfiguration, MainActivity.audioEncoding);
            short[] audiodata = new short[bufferSize / 4];

            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(notes[current_note].getSound_path())));
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, MainActivity.frequency,
                        MainActivity.channelConfiguration, MainActivity.audioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();
                while (isPlaying && dis.available() > 0)
                {
                    int i = 0;
                    while (dis.available() > 0 && i < audiodata.length) {
                        audiodata[i] = dis.readShort();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);
                } // while (isPlaying && dis.available() > 0)
                dis.close();
                isPlaying = false;
            } catch (Throwable t)
            {
                Log.e("AudioTrack", "Playback Failed");
            } // catch (Throwable t)
            return null;
        } // protected Void doInBackground(Void... params)
    } // private class PlayAudio extends AsyncTask<Void, Integer, Void>
} // public class NotesListActivity extends ListActivity
