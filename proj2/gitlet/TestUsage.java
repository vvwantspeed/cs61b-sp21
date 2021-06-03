package gitlet;

import java.io.*;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;

public class TestUsage {
    public static String path = "E:\\CS\\CS61B - Data Structures\\cs61b-sp21\\proj2";
    public static void main(String[] args) {
        String filename = "E:\\CS\\CS61B - Data Structures\\cs61b-sp21\\proj2\\gitlet\\Commit.java";
        // testPath();
        // testFile(filename);
        // testSHA1();
        // testMapString();
        // testSubDir();

       // testObject();
       // testDateFormat();

        // testListToString();
        // testSetToString();
        // testSubString();

        // testWriteContent();
        // testRemote();
        testRemotePath();
    }

    private static void testRemotePath() {
        String path = "..\\test\\.git";
        File file = new File(path);
        System.out.println(file);
        try {
            System.out.println(file.getCanonicalFile());
            System.out.println(file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
        File file = join(path, "config");
        String str = readContentsAsString(file);
        System.out.println(str);
        // ..\\remotegit\\.git
         */
    }

    private static void testRemote() {
        String str = "../remotegit/.git";
        if (File.separator.equals("\\")) {
            str = str.replaceAll("/", "\\\\\\\\");
        }
        File file = new File(str);
        System.out.println(str);    // ..\\remotegit\\.git
        System.out.println(file.exists());
    }

    private static void testWriteContent() {
        File file = join(path, "config");
        //String content = readContentsAsString(file);

        String str = "../remotegit/.git";
        if (File.separator.equals("\\")) {
            str = str.replaceAll("/", "\\\\\\\\");
        }
        writeContents(file, str);
        // 写进去是 ..\\remotegit\\.git
    }

    private static void testSubString() {
        String sha1 = sha1("sha1test");
        System.out.println(sha1);
        System.out.println(sha1.substring(0, 7));
    }

    private static void testSetToString() {
        Set<String> set = new HashSet<>();
        set.add("aa");
        set.add("bb");
        System.out.println(set);
    }

    private static void testListToString() {
        List<String> list = new ArrayList<>();
        list.add("aa");
        list.add("bb");
        String out = list.toString();   // [aa, bb]
        System.out.println(out);
    }

    private static void testDateFormat() {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        Date date = new Date();
        System.out.println(df.format(date));
    }

    private static void testObject() {
        Commit commit = new Commit();
        Repository repo = new Repository();
        File file = join(repo.CWD, commit.getID());
        writeObject(file, commit);
    }

    private static void testSubDir() {
        File file = new File(path);
        File[] files = file.listFiles();
        // ???
    }

    private static void testMapString() {
        HashMap<String, String> map = new HashMap<>();
        map.put("a", "b");
        map.put("aa", "bb");
        System.out.println(map.toString());
        // {aa=bb, a=b}
    }

    private static void testPath() {
        String master = Paths.get("refs", "heads", "master").toString();
        System.out.println(master);
        // refs\heads\master


        File file = join(path, "testing", "src", "wug.txt");
        System.out.println(file.getPath());
        System.out.println(file.getAbsolutePath());
        try {
            System.out.println(file.getCanonicalPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * filename + content && filename + contentString has the same sha1.
     * also, contentString = new String(content, StandardCharsets.UFT_8);
     * content = contentString.getBytes(StandardCharsets.UTF_8);
     * Utils.writeContents is using byte, and transform String to a byte[],
     * so, byte[] content is enough, no need for String contentString I guess.
     * when merge, maybe needed ???
     */
    private static void testSHA1(String filename) {
        File file = new File(filename);
        byte[] content = gitlet.Utils.readContents(file);
        String contentString = gitlet.Utils.readContentsAsString(file);
        //content = contentString.getBytes(StandardCharsets.UTF_8);
        // List<Object> objs = List.of(filename, content, contentString);
        // String uid = Utils.sha1(objs);
        // String uid = Utils.sha1(filename, content, contentString);
        System.out.println(gitlet.Utils.sha1(filename, content, contentString));
        System.out.println(gitlet.Utils.sha1(filename, contentString));
        System.out.println(gitlet.Utils.sha1(filename, new String(content, StandardCharsets.UTF_8)));
        System.out.println(gitlet.Utils.sha1(filename, content));
        System.out.println(gitlet.Utils.sha1(filename, content.toString()));
        System.out.println(gitlet.Utils.sha1(filename, Arrays.toString(content)));
        /*
        48d9be997d2d8248dab9d2a0b9b559e394376c22
        ade93a48def8d68f57734380e54313147bd92017
        ade93a48def8d68f57734380e54313147bd92017
        ade93a48def8d68f57734380e54313147bd92017
        5c3683d9d91d4ee352159760d218eb2262677e35
        f038fb21d5707e3bb4279c85365d35a0e884096c
        */

        /*
        System.out.println("content.toString()");
        System.out.println(content.toString());   // [B@7e0b37bc
        System.out.println("\n\n-------------Arrays.toString(content)------------\n");
        System.out.println(Arrays.toString(content));   // [112, 97, 99, 107, 97, 103, 101, 32, 103, 105, 1 ....
        System.out.println("\n\n-------------contentString--------------\n");
        System.out.println(contentString);      // actual file content
        */
    }

    private static void testFile() {
        File CWD = new File(System.getProperty("user.dir"));
        System.out.println(CWD);
        // E:\CS\CS61B - Data Structures\cs61b-sp21\proj2
        File GITLET_DIR = join(CWD, ".gitlet");
        System.out.println(GITLET_DIR);

        File home = new File(System.getProperty("user.home"));
        System.out.println(home);
        // C:\Users\volderoth
    }
}
