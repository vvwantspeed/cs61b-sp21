package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;

/**
 *  Represents a file Object.
 */
public class Blob implements Serializable {
    private String filename;
    private byte[] content;
    private String id;

    public Blob(String filename, File CWD) {
        this.filename = filename;
        File file = join(CWD, filename);
        if (file.exists()) {
            this.content = readContents(file);
            this.id = sha1(filename, content);
        } else {
            this.content = null;
            this.id = sha1(filename);
        }
    }

    public boolean exists() {
        return this.content != null;
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
