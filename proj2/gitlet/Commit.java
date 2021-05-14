package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author vv
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The timestamp of this Commit. */
    private Date timestamp;

    private String parentID;

    /** The files this Commit track. */
    // filename, blobsID
    private HashMap<String, String> blobs;

    private String id;

    /**
     * initial commit.
     */
    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.id = Utils.sha1(message, timestamp.toString());
        this.parentID = "null";
    }

    public Commit(String message, Commit parent, Stage stage) {
        this.message = message;
        this.timestamp = new Date();
        this.parentID = parent.getID();
        this.blobs = parent.getBlobs();

        for (Map.Entry<String, String> item : stage.getAdded().entrySet()) {
            String filename = item.getKey();
            String blobId = item.getValue();
            blobs.put(filename, blobId);
        }
        for (String filename : stage.getRemoved()) {
            blobs.remove(filename);
        }

        this.id = Utils.sha1(message, timestamp.toString(), parentID, blobs.toString());
    }

    public void writeToFile() {
        File file = Utils.join(Repository.COMMITS_DIR, id);
        Utils.writeObject(file, this);
    }

    public String getParentID() {
        return this.parentID;
    }

    public String getID() {
        return id;
    }

    public String getDateString() {
        // Thu Nov 9 20:00:05 2017 -0800
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy xxxx", Locale.ENGLISH);
        return df.format(timestamp);
    }

    public String getMessage() {
        return this.message;
    }

    public HashMap<String, String> getBlobs() {
        return this.blobs;
    }

    public String getCommitAsString() {
        StringBuffer sb = new StringBuffer();
        sb.append("===\n");
        sb.append("commit " + this.id + "\n");
        sb.append("Date " + this.getDateString() + "\n");
        sb.append(this.message + "\n\n");
        return sb.toString();
    }
}
