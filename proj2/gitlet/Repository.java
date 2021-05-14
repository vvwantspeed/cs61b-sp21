package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.*;

// TODO: any imports you need here

/**
 * Represents a gitlet repository.
 * TODO: It's a good idea to give a description here of what else this Class
 * does at a high level.
 *
 * @author vv
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * .gitlet
     *     -- staging
     *     -- [stage]
     *     -- blobs
     *     -- commits
     *     -- refs
     *         -- heads -> [master]
     *         -- remotes // reserved
     *     -- [HEAD]
     *     -- [DETACHEDHEAD]
     */

    /**
     * The staging directory, restores staging Blobs
     */
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    /**
     * The Stage Object
     */
    public static final File STAGE = join(GITLET_DIR, "stage");

    /**
     * The Objects directory, stores committed blobs & commits
     */
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");

    /**
     * The branches directory
     */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    /**
     * The current Commit,
     * refs: relative path of tips of the branch
     * detached: sha1 of the Commit
     */
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    // Note that in Gitlet, there is no way to be in a detached head state

    /**
     * stores current branch's name
     */
    public static final File BRANCH = join(GITLET_DIR, "branch");

    public static void init() {
        // Failure cases
        if (GITLET_DIR.exists() && GITLET_DIR.isDirectory()) {
            System.out.println("A Gitlet version-control system already exists in the current directory");
            System.exit(0);
        }

        // create directories
        GITLET_DIR.mkdir();
        STAGING_DIR.mkdir();
        writeObject(STAGE, new Stage());
        BLOBS_DIR.mkdir();
        COMMITS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();

        // initial commit
        Commit initialCommit = new Commit();
        writeCommitToFile(initialCommit);
        String id = initialCommit.getID();

        // create branch: master
        String branchname = "master";
        writeContents(BRANCH, branchname);
        File master = join(HEADS_DIR, branchname);
        writeContents(master, id);

        // create HEAD
        writeContents(HEAD, branchname);
    }

    /**
     * 1. Staging an already-staged file overwrites the previous entry in the staging area with the new contents.
     * 2. If the current working version of the file is identical to the version in the current commit,
     * do not stage it to be added, and remove it from the staging area if it is already there
     * (as can happen when a file is changed, added, and then changed back to it’s original version).
     * 3. TODO: The file will no longer be staged for removal (see gitlet rm), if it was at the time of the command.
     *
     * @param filename
     */
    public static void add(String filename) {
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Commit head = getHead();
        String headId = head.getBlobs().getOrDefault(filename, "");

        Stage stage = readStage();
        String stageId = stage.getAdded().getOrDefault(filename, "");

        Blob blob = new Blob(filename);
        String blobId = blob.getId();

        if (headId.equals(blobId)) {
            // no need to add the file
            if (!stageId.equals("")) {
                // del the file from staging
                join(STAGING_DIR, stageId).delete();
                stage.getAdded().remove(stageId);
                writeStage(stage);
            }
        } else if (!stageId.equals(blobId)) {
            // update staging
            // del original, add the new version
            if (!stageId.equals("")) {
                join(STAGING_DIR, stageId).delete();
            }

            writeObject(join(STAGING_DIR, blobId), blob);
            // change stage added files
            stage.addFile(filename, blobId);
            writeStage(stage);
        }
    }

    public static void rm(String filename) {
        File file = join(CWD, filename);

        Commit head = getHead();
        String headId = head.getBlobs().getOrDefault(filename, "");

        Stage stage = readStage();
        String stageId = stage.getAdded().getOrDefault(filename, "");

        Blob blob = new Blob(filename);
        String blobId = blob.getId();

        if (headId.equals("") || stageId.equals("")) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // Unstage the file if it is currently staged for addition.
        if (!stageId.equals("")) {
            stage.getAdded().remove(filename);
        }
        // If the file is tracked in the current commit
        if (!headId.equals("")) {
            // stage it for removal
            stage.getRemoved().add(filename);
            // remove the file from the working directory
            // if the user has not already done so
            restrictedDelete(file);
        }
    }

    /**
     *
     */
    public static void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Stage stage = readStage();

        // If no files have been staged, abort.
        if (stage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit head = getHead();
        Commit commit = new Commit(message, head, stage);
        clearStage(stage);
        writeCommitToFile(commit);

        String commitId = commit.getID();
        File branch = getCurrentBranch();
        writeContents(branch, commitId);
    }


    public static void log() {
        StringBuffer sb = new StringBuffer();
        Commit commit = getHead();
        while (commit != null) {
            sb.append(commit.getCommitAsString());
            commit = getCommitFromId(commit.getParentID());
        }

        System.out.print(sb);
    }

    public static void global_log() {
        StringBuffer sb = new StringBuffer();
        List<String> filenames = plainFilenamesIn(COMMITS_DIR);
        for (String filename : filenames) {
            Commit commit = getCommitFromId(filename);
            sb.append(commit.getCommitAsString());
        }
        System.out.println(sb);
    }

    public static void find(String target) {
        StringBuffer sb = new StringBuffer();
        List<String> filenames = plainFilenamesIn(COMMITS_DIR);
        for (String filename : filenames) {
            Commit commit = getCommitFromId(filename);
            if (commit.getMessage().contains(target)) {
                sb.append(commit.getID() + "\n");
            }
        }
        if (sb.length() == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        System.out.println(sb);
    }

    public static void status() {
        StringBuffer sb = new StringBuffer();
        sb.append("=== Branches ===\n");
        String currentBranch = readContentsAsString(BRANCH);
        sb.append("*" + currentBranch + "\n");
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        for (String branch : branches) {
            if (!branch.equals(currentBranch)) {
                sb.append(branch + "\n");
            }
        }
        sb.append("\n");

        Stage stage = readStage();
        sb.append("=== Staged Files ===\n");
        for (String filename : stage.getAdded().keySet()) {
            sb.append(filename + "\n");
        }
        sb.append("\n");

        sb.append("=== Removed Files ===\n");
        for (String filename : stage.getRemoved()) {
            sb.append(filename + "\n");
        }
        sb.append("\n");

        sb.append("=== Modifications Not Staged For Commit ===\n");
        sb.append("\n");

        sb.append("=== Untracked Files ===\n");
        sb.append("\n");

        System.out.println(sb);
    }

    /**
     * java gitlet.Main checkout -- [file name]
     */
    public static void checkoutFileFromHead(String filename) {
        Commit head = getHead();
        checkoutFileFromCommit(head, filename);
    }

    /**
     * java gitlet.Main checkout [commit id] -- [file name]
     *
     * @return
     */
    public static void checkoutFileFromCommitId(String commitId, String filename) {
        File commitFile = join(COMMITS_DIR, commitId);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
        }
        Commit commit = readObject(commitFile, Commit.class);
        checkoutFileFromCommit(commit, filename);
    }

    private static void checkoutFileFromCommit(Commit commit, String filename) {
        String blobId = commit.getBlobs().getOrDefault(filename, "");
        if (blobId.equals("")) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);
        File file = join(CWD, blob.getFilename());
        writeContents(file, blob.getContent());
    }

    /**
     * java gitlet.Main checkout [branch name]
     */
    public static void checkoutBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        validUntrackedFile();

        clearStage(readStage());
        // String headCommitId = getCommitIdFromBranchName(headBranchName);
        Commit commit = getCommitFromBranchName(branchName);
        replaceWorkingPlaceWithCommit(commit);

        // change HEAD point to this branch
        writeContents(HEAD, branchName);
    }


    /**
     * java gitlet.Main branch [branch name]
     * create a new branch with name [branchName]
     */
    public static void branch(String branchName) {
        File branch = join(HEADS_DIR, branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String commitId = getHeadCommitId();
        writeContents(branch, commitId);

        writeContents(HEAD, branchName);
    }

    /**
     * java gitlet.Main rm-branch [branch name]
     */
    public static void rmBranch(String branchName) {
        File branch = join(HEADS_DIR, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        branch.delete();
    }

    public static void reset(String commitId) {
        File file = join(COMMITS_DIR, commitId);
        if (!file.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        validUntrackedFile();

        Commit commit = getCommitFromId(commitId);
        replaceWorkingPlaceWithCommit(commit);
        clearStage(readStage());

        // moves the current branch’s head to that commit node.
        String currentBranchName = getHeadBranchName();
        writeContents(join(HEADS_DIR, currentBranchName), commitId);
    }



    /**
     * Helper Function
     */

    private static void validUntrackedFile() {
        List<String> untrackedFiles = getUntrackedFiles();
        if (!untrackedFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
    }

    private static void replaceWorkingPlaceWithCommit(Commit commit) {
        clearWorkingSpace();
        for (Map.Entry<String, String> item : commit.getBlobs().entrySet()) {
            String filename = item.getKey();
            String blobId = item.getValue();
            File file = join(CWD, filename);
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);

            writeContents(file, blob.getContent());
        }
    }

    private static void clearWorkingSpace() {
        File[] files = CWD.listFiles(gitletFliter);
        for (File file : files) {
            delFile(file);
        }
    }

    private static void delFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delFile(f);
            }
        }
        file.delete();
    }

    private static FilenameFilter gitletFliter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(".gitlet") || !new File(dir, name).isDirectory();
        }
    };

    private static List<File> getWorkingSpaceFiles() {
        File[] files = CWD.listFiles(gitletFliter);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    private static List<String> getUntrackedFiles() {
        List<String> res = new ArrayList<>();
        List<String> stageFiles = readStage().getStagedFilename();
        Set<String> headFiles = getHead().getBlobs().keySet();
        for (String filename : plainFilenamesIn(CWD)) {
            if (!stageFiles.contains(filename) && !headFiles.contains(filename)) {
                res.add(filename);
            }
        }
        return res;
    }

    private static File getCurrentBranch() {
        return join(HEADS_DIR, readContentsAsString(BRANCH));   // ..\refs\heads\<branch name>
    }

    private static void clearStage(Stage stage) {
        File[] files = STAGING_DIR.listFiles();
        if (files == null) {
            return;
        }
        Path targetDir = BLOBS_DIR.toPath();
        for (File file : files) {
            Path source = file.toPath();
            try {
                Files.move(source, targetDir.resolve(source.getFileName())); // ATOMIC_MOVE);//REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        stage = new Stage();
        writeStage(new Stage());
    }

    private static Stage readStage() {
        return readObject(STAGE, Stage.class);
    }

    private static void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    private static String getHeadBranchName() {
        return readContentsAsString(HEAD);
    }

    private static String getCommitIdFromBranchName(String branchName) {
        File tip = join(HEADS_DIR, branchName);
        return readContentsAsString(tip);
    }

    private static Commit getCommitFromId(String commitId) {
        File file = join(COMMITS_DIR, commitId);
        if (commitId.equals("null") || !file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    private static Commit getCommitFromBranchName(String branchName) {
        String commitId = getCommitIdFromBranchName(branchName);
        return getCommitFromId(commitId);
    }

    private static String getHeadCommitId() {
        String branchName = getHeadBranchName();
        return getCommitIdFromBranchName(branchName);
    }

    private static Commit getHead() {
        String branchName = getHeadBranchName();
        Commit head = getCommitFromBranchName(branchName);

        if (head == null) {
            System.out.println("error! cannot find HEAD!");
            System.exit(0);
        }

        return head;
    }

    private static void writeCommitToFile(Commit commit) {
        File file = join(Repository.COMMITS_DIR, commit.getID());
        writeObject(file, commit);
    }


    /**
     * check things
     */
    static void checkIfInitDirectoryExists() {
        if (!GITLET_DIR.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    static void checkCommandLength(int actual, int expected) {
        if (actual != expected) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
