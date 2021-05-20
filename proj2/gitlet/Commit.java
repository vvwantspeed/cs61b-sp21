package gitlet;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author vv
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The timestamp of this Commit. */
    private Date timestamp;

    private List<String> parents;

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
        this.id = sha1(message, timestamp.toString());
        this.parents = new LinkedList<>();
        this.blobs = new HashMap<>();
    }

    public Commit(String message, List<Commit> parents, Stage stage) {
        this.message = message;
        this.timestamp = new Date();
        this.parents = new ArrayList<>(2);
        for (Commit p : parents) {
            this.parents.add(p.getID());
        }
        // using first parent blobs
        this.blobs = parents.get(0).getBlobs();

        for (Map.Entry<String, String> item : stage.getAdded().entrySet()) {
            String filename = item.getKey();
            String blobId = item.getValue();
            blobs.put(filename, blobId);
        }
        for (String filename : stage.getRemoved()) {
            blobs.remove(filename);
        }

        this.id = sha1(message, timestamp.toString(), parents.toString(), blobs.toString());
    }

    public List<String> getParents() {
        return parents;
    }

    public String getFirstParentId() {
        if (parents.isEmpty()) {
            return "null";
        }
        return parents.get(0);
    }

    public String getID() {
        return id;
    }

    public String getDateString() {
        // Thu Nov 9 20:00:05 2017 -0800
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
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
        if (parents.size() == 2) {
            sb.append("Merge: " + parents.get(0).substring(0, 7) + " " + parents.get(1).substring(0, 7) + "\n");
        }
        sb.append("Date: " + this.getDateString() + "\n");
        sb.append(this.message + "\n\n");
        return sb.toString();
    }
}
