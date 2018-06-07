package pl.druci.sixthlab;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity
{
    private RecordAudio recordTask;
    private Button startRecordingButton, stopRecordingButton, resetRecordingButton,
            storeRecordingButton, listRecordingButton;
    private EditText name, surname, title, description;
    private File recordingFile;
    private File metaFile;
    public static File path;

    private boolean isRecording = false, isStopped = false;

    static int frequency = 11025,channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    static int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRecordingButton = (Button) findViewById(R.id.start);
        stopRecordingButton = (Button) findViewById(R.id.stop);
        resetRecordingButton = (Button) findViewById(R.id.reset);
        storeRecordingButton = (Button) findViewById(R.id.store);
        listRecordingButton = (Button) findViewById(R.id.list);

        name = (EditText) findViewById(R.id.name);
        surname = (EditText) findViewById(R.id.surname);
        title = (EditText) findViewById(R.id.title);
        description = (EditText) findViewById(R.id.description);

        stopRecordingButton.setEnabled(false);
        resetRecordingButton.setEnabled(false);
        storeRecordingButton.setEnabled(false);
        listRecordingButton.setEnabled(true);

        path = new File(Environment.getExternalStorageDirectory() + getString(R.string.app_folder_name));
        File master_path = new File(Environment.getExternalStorageDirectory() + getString(R.string.app_master_folder_name));
        master_path.mkdir();
        path.mkdir();
    } // public void onCreate(Bundle savedInstanceState)

    private void createNewFile()
    {
        try
        {
            recordingFile = File.createTempFile("recording", ".pcm", path);
        } catch (IOException e)
        {
            throw new RuntimeException("Couldn't create file on SD card", e);
        } // catch (IOException e)
    } // private void createNewFile()

    private void createNewMetaDataFile()
    {
        try
        {
            StringBuilder builder = new StringBuilder(recordingFile.getAbsolutePath());
            builder.replace(builder.length()-3, builder.length(), "txt");
            metaFile = new File(builder.toString());
            if(!metaFile.exists())
                metaFile.createNewFile();
        } catch (IOException e)
        {
            throw new RuntimeException("Couldn't create file on SD card", e);
        } // catch (IOException e)
    } // private void createNewMetaDataFile()

    public void vOnClickedStart(View cView)
    {
        record();
    } // public void vOnClickedStart(View cView)

    public void vOnClickedStop(View cView)
    {
        stopRecording();
        isStopped = true;
    } // public void vOnClickedStop(View cView)

    public void vOnClickedReset(View cView)
    {
        vClearTexts();

        recordingFile.delete();
        metaFile.delete();
        listRecordingButton.setEnabled(true);
        resetRecordingButton.setEnabled(false);
        storeRecordingButton.setEnabled(false);
    } // public void vOnClickedReset(View cView)

    public void vOnClickedStore(View cView)
    {
        stopRecording();
        isStopped = false;

        String s_name = name.getText().toString();
        String s_surname = surname.getText().toString();
        String s_title = title.getText().toString();
        String s_description = description.getText().toString();

        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile));
            writer.write(s_name);
            writer.newLine();
            writer.write(s_surname);
            writer.newLine();
            writer.write(s_title);
            writer.newLine();
            writer.write(s_description);
            writer.close();
        } catch (IOException e)
        {
            Log.e("MetaData", "Writer Failed");
        } // catch (IOException e)

        vClearTexts();
        listRecordingButton.setEnabled(true);
        resetRecordingButton.setEnabled(false);
        storeRecordingButton.setEnabled(false);
    } // public void vOnClickedStore(View cView)

    public void vOnClickedList(View cView)
    {
        Intent intent = new Intent(this, NotesListActivity.class);
        startActivity(intent);
    } // public void vOnClickedList(View cView)

    public void record()
    {
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);
        listRecordingButton.setEnabled(false);
        recordTask = new RecordAudio();
        recordTask.execute();
    } // public void record()

    public void stopRecording()
    {
        isRecording = false;
    } // public void stopRecording()

    private void vClearTexts()
    {
        name.getText().clear();
        surname.getText().clear();
        title.getText().clear();
        description.getText().clear();
    } // private void vClearTexts()


    private class RecordAudio extends AsyncTask<Void, Integer, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            isRecording = true;
            try {
                DataOutputStream dos;

                if (isStopped)
                    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile, true)));
                else
                {
                    createNewFile();
                    createNewMetaDataFile();
                    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                } // if (!isStopped)

                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[bufferSize];
                audioRecord.startRecording();
                int r = 0;
                while (isRecording)
                {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            bufferSize);

                    for (int ii = 0; ii < bufferReadResult; ii++)
                    {
                        dos.writeShort(buffer[ii]);
                    } // for (int ii = 0; ii < bufferReadResult; ii++)

                    publishProgress(new Integer(r));
                    r++;
                } // while (isRecording)
                audioRecord.stop();
                dos.close();
            } catch (Throwable t)
            {
                Log.e("AudioRecord", "Recording Failed");
            } // catch (Throwable t)
            return null;
        } // protected Void doInBackground(Void... params)

        protected void onPostExecute(Void result)
        {
            startRecordingButton.setEnabled(true);
            stopRecordingButton.setEnabled(false);
            resetRecordingButton.setEnabled(true);
            storeRecordingButton.setEnabled(true);
        } // protected void onPostExecute(Void result)
    } // private class RecordAudio extends AsyncTask<Void, Integer, Void>
} // public class MainActivity extends Activity
