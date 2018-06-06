package pl.druci.sixthlab;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by oladr on 20.05.2018.
 */

public class Note
{
    private String name;
    private String surname;
    private String title;
    private String description;
    private String path;
    private File sound_path;
    private String modification;

    public Note(String path)
    {
        this.path = path;
        setSound_path();

        vSetModification();
    } // public Notes(String path)

    private void vSetModification()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        modification = sdf.format(sound_path.lastModified());
    } // private void vSetModyfication()

    private void setSound_path()
    {
        StringBuilder builder = new StringBuilder(path);
        builder.replace(builder.length()-3, builder.length(), "pcm");
        sound_path = new File(builder.toString());
    } // private void setSound_path()

    private void vUpdate()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(name);
            writer.newLine();
            writer.write(surname);
            writer.newLine();
            writer.write(title);
            writer.newLine();
            writer.write(description);
            writer.close();
        } catch (IOException e)
        {
            Log.e("MetaData", "Writer Failed");
        } // catch (IOException e)
    } // public void vUpdate()

    public void vAddMeta(String sName, String sSurname, String sTitle, String sDescription)
    {
        name += " + " + sName;
        surname += " + " + sSurname;
        title += " + " + sTitle;
        description += " + " + sDescription;
        vUpdate();
    } // public void vAddMeta(String sName, String sSurname, String sTitle, String sDescription)

    public void vReadFromFile()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            name = reader.readLine();
            surname = reader.readLine();
            title = reader.readLine();
            description = reader.readLine();
            reader.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        } // catch (IOException e)
    } // public void vReadFromFile()

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public File getSound_path() {
        return sound_path;
    }

    @Override
    public String toString() {
        return title + " - " + name + " " + surname + " (" + modification + ")";
    }
} // public class Notes
