package gitlet;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 *  Represents a file Object.
 */
public class Blob implements Serializable {
    private String filename;
    private byte[] content;
    private String id;

    public Blob(String filename) {
        this.filename = filename;
        this.content = Utils.readContents(Utils.join(Repository.CWD, filename));
        this.id = Utils.sha1(filename, content);
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    public String getId() {
        return id;
    }
}
