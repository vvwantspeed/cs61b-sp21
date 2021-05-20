package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.server.UID;
import java.util.*;

import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author vv
 */
public class Repository {
    /**
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
     * <p>
     * .gitlet
     * -- staging
     * -- [stage]
     * -- blobs
     * -- commits
     * -- refs
     * -- heads -> [master][branch name]
     * -- remotes // reserved
     * -- [HEAD]
     * -- [branch]
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

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
     * stores current branch's name if it points to tip
     */
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    // Note that in Gitlet, there is no way to be in a detached head state


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
        String branchName = "master";
        writeContents(HEAD, branchName);
        File master = join(HEADS_DIR, branchName);
        writeContents(master, id);

        // create HEAD
        writeContents(HEAD, branchName);
    }

    /**
     * 1. Staging an already-staged file overwrites the previous entry in the staging area with the new contents.
     * 2. If the current working version of the file is identical to the version in the current commit,
     * do not stage it to be added, and remove it from the staging area if it is already there
     * (as can happen when a file is changed, added, and then changed back to it’s original version).
     * 3. The file will no longer be staged for removal (see gitlet rm), if it was at the time of the command.
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
        Stage stage = readStage();
        // blob id
        String headId = head.getBlobs().getOrDefault(filename, "");
        String stageId = stage.getAdded().getOrDefault(filename, "");

        Blob blob = new Blob(filename);
        String blobId = blob.getId();

        if (blobId.equals(headId)) {
            // no need to add the file
            if (!blobId.equals(stageId)) {
                // del the file from staging
                join(STAGING_DIR, stageId).delete();
                stage.getAdded().remove(stageId);
                stage.getRemoved().remove(filename);
                writeStage(stage);
            }
        } else if (!blobId.equals(stageId)) {
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
        Stage stage = readStage();

        // blob id
        String headId = head.getBlobs().getOrDefault(filename, "");
        String stageId = stage.getAdded().getOrDefault(filename, "");

        if (headId.equals("") && stageId.equals("")) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }


        // Unstage the file if it is currently staged for addition.
        if (!stageId.equals("")) {
            stage.getAdded().remove(filename);
        } else {
            // stage it for removal
            stage.getRemoved().add(filename);
        }

        Blob blob = new Blob(filename);
        String blobId = blob.getId();
        // If the file is tracked in the current commit
        // the same content? or just filename?
        if (blob.exists() && blobId.equals(headId)) {
            // remove the file from the working directory
            // if the user has not already done so
            restrictedDelete(file);
        }

        writeStage(stage);
    }

    /**
     *
     */
    public static void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Commit head = getHead();
        commitWith(message, List.of(head));
    }


    public static void log() {
        StringBuffer sb = new StringBuffer();
        Commit commit = getHead();
        while (commit != null) {
            sb.append(commit.getCommitAsString());
            commit = getCommitFromId(commit.getFirstParentId());
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
        String headBranch = readContentsAsString(HEAD);
        sb.append("*" + headBranch + "\n");
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        for (String branch : branches) {
            if (!branch.equals(headBranch)) {
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
        commitId = getCompleteCommitId(commitId);
        File commitFile = join(COMMITS_DIR, commitId);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = readObject(commitFile, Commit.class);
        checkoutFileFromCommit(commit, filename);
    }

    /**
     * short id
     * starts with the same six digits
     */
    private static String getCompleteCommitId(String commitId) {
        if (commitId.length() == UID_LENGTH) {
            return commitId;
        }

        for (String filename : COMMITS_DIR.list()) {
            if (filename.startsWith(commitId)) {
                return filename;
            }
        }
        return null;
    }

    private static void checkoutFileFromCommit(Commit commit, String filename) {
        String blobId = commit.getBlobs().getOrDefault(filename, "");
        checkoutFileFromBlobId(blobId);
    }

    private static void checkoutFileFromBlobId(String blobId) {
        if (blobId.equals("")) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = getBlobFromId(blobId);
        checkoutFileFromBlob(blob);
    }

    private static void checkoutFileFromBlob(Blob blob) {
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

        Commit otherCommit = getCommitFromBranchName(branchName);

        // If a working file is untracked in the current branch
        // and would be overwritten by the checkout
        validUntrackedFile(otherCommit.getBlobs());

        clearStage(readStage());
        replaceWorkingPlaceWithCommit(otherCommit);

        // change HEAD point to this branch
        writeContents(HEAD, branchName);
    }


    /**
     * java gitlet.Main branch [branch name]
     * create a new branch with name [branchName]
     * This command does NOT immediately switch to the newly created branch
     * (just as in real Git).
     */
    public static void branch(String branchName) {
        File branch = join(HEADS_DIR, branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String commitId = getHeadCommitId();
        writeContents(branch, commitId);
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

        Commit commit = getCommitFromId(commitId);

        validUntrackedFile(commit.getBlobs());

        replaceWorkingPlaceWithCommit(commit);
        clearStage(readStage());

        // moves the current branch’s head to that commit node.
        String headBranchName = getHeadBranchName();
        writeContents(join(HEADS_DIR, headBranchName), commitId);
    }

    /**
     * java gitlet.Main merge [branch name]
     */
    public static void merge(String otherBranchName) {
        // failure cases:
        Stage stage = readStage();
        if (!stage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        File branchFile = join(HEADS_DIR, otherBranchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(otherBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit head = getCommitFromBranchName(headBranchName);
        Commit other = getCommitFromBranchName(otherBranchName);
        Commit lca = getLatestCommonAncestor(head, other);

        // 1. lca == given a.k.a given <-- current
        if (lca.getID().equals(other.getID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // 2. lca == head a.k.a current <-- given
        // checkout
        if (lca.getID().equals(head.getID())) {
            checkoutBranch(otherBranchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // 3. merge
        mergeWithLCA(lca, head, other);

        String msg = "Merged " + otherBranchName + " into " + headBranchName + ".";
        List<Commit> parents = List.of(head, other);
        commitWith(msg, parents);
    }


    /**
     * Helper Functions
     */

    private static void commitWith(String message, List<Commit> parents) {
        Stage stage = readStage();
        // If no files have been staged, abort.
        if (stage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit commit = new Commit(message, parents, stage);
        clearStage(stage);
        writeCommitToFile(commit);

        String commitId = commit.getID();
        File branch = getHeadBranchFile();
        writeContents(branch, commitId);
    }


    // The split point is a latest common ancestor of the current and given branch heads
    private static Commit getLatestCommonAncestor(Commit head, Commit other) {
        Set<String> headAncestors = bfsFromCommit(head);

        Queue<Commit> queue = new LinkedList<>();
        queue.add(other);
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            if (headAncestors.contains(commit.getID())) {
                return commit;
            }
            if (!commit.getParents().isEmpty()) {
                for (String id : commit.getParents()) {
                    queue.add(getCommitFromId(id));
                }
            }
        }
        return new Commit();
    }

    private static Set<String> bfsFromCommit(Commit head) {
        Set<String> res = new HashSet<>();
        Queue<Commit> queue = new LinkedList<>();
        queue.add(head);
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            if (!res.contains(commit.getID()) && !commit.getParents().isEmpty()) {
                for (String id : commit.getParents()) {
                    queue.add(getCommitFromId(id));
                }
            }
            res.add(commit.getID());
        }
        return res;
    }

    private static void mergeWithLCA(Commit lca, Commit head, Commit other) {
        Set<String> filenames = getAllFilenames(lca, head, other);

        List<String> remove = new LinkedList<>();
        List<String> rewrite = new LinkedList<>();
        List<String> conflict = new LinkedList<>();

        for (String filename : filenames) {
            // blobId
            String lId = lca.getBlobs().getOrDefault(filename, "");
            String hId = head.getBlobs().getOrDefault(filename, "");
            String oId = other.getBlobs().getOrDefault(filename, "");

            if (hId.equals(oId) || lId.equals(oId)) {
                continue;
            }
            if (lId.equals(hId)) {
                // change the file to other version
                if (oId.equals("")) {
                    // remove the file
                    // rm(filename);
                    remove.add(filename);
                } else {
                    // rewrite working space's file with other version
                    // checkoutFileFromBlobId(oId);
                    // Blob blob = getBlobFromId(oId);
                    // checkoutFileFromBlob(blob);
                    // // add the file
                    // add(filename);
                    rewrite.add(filename);
                }
            } else {
                // conflict
                conflict.add(filename);
            }
        }


        // If an untracked file in the current commit would be overwritten or deleted by the merge
        List<String> untrackedFiles = getUntrackedFiles();
        for (String filename : untrackedFiles) {
            if (remove.contains(filename) || rewrite.contains(filename) || conflict.contains(filename)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        if (!remove.isEmpty()) {
            for (String filename : remove) {
                rm(filename);
            }
        }

        if (!rewrite.isEmpty()) {
            for (String filename : rewrite) {
                String oId = other.getBlobs().getOrDefault(filename, "");
                Blob blob = getBlobFromId(oId);
                checkoutFileFromBlob(blob);
                // add the file
                add(filename);
            }
        }

        if (!conflict.isEmpty()) {
            for (String filename : conflict) {
                // blobId
                String hId = head.getBlobs().getOrDefault(filename, "");
                String oId = other.getBlobs().getOrDefault(filename, "");

                String headContent = getContentAsStringFromBlobId(hId);
                String otherContent = getContentAsStringFromBlobId(oId);
                String content = getConflictFile(headContent.split("\n"),
                        otherContent.split("\n"));
                rewriteFile(filename, content);
                System.out.println("Encountered a merge conflict.");
            }
        }

        /** TODO ? :
         * 3. If a file was removed from both the current and given branch,
         * but a file of the same name is present in the working directory,
         * it is left alone and continues to be absent (not tracked nor staged) in the merge.
         */
    }

    private static String getContentAsStringFromBlobId(String blobId) {
        if (blobId.equals("")) {
            return "";
        }
        return getBlobFromId(blobId).getContentAsString();
    }


    private static String getConflictFile(String[] head, String[] other) {
        StringBuffer sb = new StringBuffer();
        int len1 = head.length, len2 = other.length;
        int i = 0, j = 0;
        while (i < len1 && j < len2) {
            if (head[i].equals(other[j])) {
                sb.append(head[i]);
            } else {
                sb.append(getConflictContent(head[i], other[j]));
            }
            i++;
            j++;
        }
        // head.len > other.len
        while (i < len1) {
            sb.append(getConflictContent(head[i], ""));
            i++;
        }
        // head.len < other.len
        while (j < len1) {
            sb.append(getConflictContent("", other[j]));
            j++;
        }
        return sb.toString();
    }

    private static String getConflictContent(String head, String other) {
        StringBuffer sb = new StringBuffer();
        sb.append("<<<<<<< HEAD\n");
        // contents of file in current branch
        sb.append(head);
        sb.append("\n=======\n");
        // contents of file in given branch
        sb.append(other);
        sb.append("\n>>>>>>>\n");
        return sb.toString();
    }

    private static void rewriteFile(String filename, String content) {
        File file = join(CWD, filename);
        writeContents(file, content);
    }

    /**
     * be sure that blob id is not "".
     */
    private static Blob getBlobFromId(String blobId) {
        File file = join(BLOBS_DIR, blobId);
        return readObject(file, Blob.class);
    }

    private static Set<String> getAllFilenames(Commit lca, Commit head, Commit other) {
        Set<String> set = new HashSet<>();
        set.addAll(lca.getBlobs().keySet());
        set.addAll(head.getBlobs().keySet());
        set.addAll(other.getBlobs().keySet());
        return set;
    }

    /**
     * If a working file is untracked in the current branch
     * and would be overwritten by the blobs(checkout).
     */
    private static void validUntrackedFile(Map<String, String> blobs) {
        List<String> untrackedFiles = getUntrackedFiles();
        if (untrackedFiles.isEmpty()) {
            return;
        }

        for (String filename : untrackedFiles) {
            String blobId = new Blob(filename).getId();
            String otherId = blobs.getOrDefault(filename, "");
            if (!otherId.equals(blobId)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
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
            return !name.equals(".gitlet");
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


    private static File getHeadBranchFile() {
        return join(HEADS_DIR, readContentsAsString(HEAD));   // ..\refs\heads\<branch name>
    }

    /**
     * moving all staging dir's blob file to blobs dir.
     *
     * @param stage
     */
    private static void clearStage(Stage stage) {
        File[] files = STAGING_DIR.listFiles();
        if (files == null) {
            return;
        }
        Path targetDir = BLOBS_DIR.toPath();
        for (File file : files) {
            Path source = file.toPath();
            try {
                Files.move(source, targetDir.resolve(source.getFileName()), REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
            messageIncorrectOperands();
        }
    }

    static void checkEqual(String actual, String expected) {
        if (!actual.equals(expected)) {
            messageIncorrectOperands();
        }
    }

    static void messageIncorrectOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }
}
