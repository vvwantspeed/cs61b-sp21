package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public File CWD;

    public Repository() {
        this.CWD = new File(System.getProperty("user.dir"));
        configDIRS();
    }

    public Repository(String cwd) {
        this.CWD = new File(cwd);
        configDIRS();
    }

    private void configDIRS() {
        this.GITLET_DIR = join(CWD, ".gitlet");
        this.STAGING_DIR = join(GITLET_DIR, "staging");
        this.STAGE = join(GITLET_DIR, "stage");
        this.BLOBS_DIR = join(GITLET_DIR, "blobs");
        this.COMMITS_DIR = join(GITLET_DIR, "commits");
        this.REFS_DIR = join(GITLET_DIR, "refs");
        this.HEADS_DIR = join(REFS_DIR, "heads");
        this.REMOTES_DIR = join(REFS_DIR, "remotes");
        this.HEAD = join(GITLET_DIR, "HEAD");
        this.CONFIG = join(GITLET_DIR, "config");
    }

    /**
     * The .gitlet directory.
     * <p>
     * .gitlet
     * -- staging
     * -- [stage]
     * -- blobs
     * -- commits
     * -- refs
     *  -- heads -> [master][branch name]
     *  -- remotes
     *      -- [remote name] ->[branch name]
     * -- [HEAD]
     * -- [config]
     */
    public File GITLET_DIR;

    /**
     * The staging directory, restores staging Blobs
     */
    public File STAGING_DIR;

    /**
     * The Stage Object
     */
    public File STAGE;

    /**
     * The Objects directory, stores committed blobs & commits
     */
    public File BLOBS_DIR;
    public File COMMITS_DIR;

    /**
     * The branches directory
     */
    public File REFS_DIR;
    public File HEADS_DIR;
    public File REMOTES_DIR;
    /**
     * stores current branch's name if it points to tip
     */
    public File HEAD;
    // Note that in Gitlet, there is no way to be in a detached head state

    public File CONFIG;

    public void init() {
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
        REMOTES_DIR.mkdir();

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

        writeContents(CONFIG, "");
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
    public void add(String filename) {
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

        Blob blob = new Blob(filename, CWD);
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

    public void rm(String filename) {
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

        Blob blob = new Blob(filename, CWD);
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
    public void commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Commit head = getHead();
        commitWith(message, List.of(head));
    }


    public void log() {
        StringBuffer sb = new StringBuffer();
        Commit commit = getHead();
        while (commit != null) {
            sb.append(commit.getCommitAsString());
            commit = getCommitFromId(commit.getFirstParentId());
        }

        System.out.print(sb);
    }

    public void global_log() {
        StringBuffer sb = new StringBuffer();
        List<String> filenames = plainFilenamesIn(COMMITS_DIR);
        for (String filename : filenames) {
            Commit commit = getCommitFromId(filename);
            sb.append(commit.getCommitAsString());
        }
        System.out.println(sb);
    }

    public void find(String target) {
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

    public void status() {
        StringBuffer sb = new StringBuffer();

        sb.append("=== Branches ===\n");
        String headBranch = readContentsAsString(HEAD);
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        for (String branch : branches) {
            if (branch.equals(headBranch)) {
                sb.append("*" + headBranch + "\n");
            } else {
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
        // List<String> modifiedFiles = getModifiedFiles(getCommitFromBranchName(headBranch), stage);
        // for (String str : modifiedFiles) {
        //     sb.append(str + "\n");
        // }
        sb.append("\n");

        sb.append("=== Untracked Files ===\n");
        // List<String> untrackedFiles = getUntrackedFiles();
        // for (String filename : untrackedFiles) {
        //     sb.append(filename + "\n");
        // }
        sb.append("\n");

        System.out.println(sb);
    }

    private List<String> getModifiedFiles(Commit head, Stage stage) {
        List<String> res = new LinkedList<>();

        List<String> currentFiles = plainFilenamesIn(CWD);
        Set<String> headFiles = head.getBlobs().keySet();
        List<String> stagedFiles = stage.getStagedFilename();

        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(currentFiles);
        allFiles.addAll(headFiles);
        allFiles.addAll(stagedFiles);

        for (String filename : allFiles) {
            if (!currentFiles.contains(filename)) {
                if (stage.getAdded().containsKey(filename) ||
                   (headFiles.contains(filename) && !stagedFiles.contains(filename))) {
                    res.add(filename + " (deleted)");
                }
            } else {
                String bId = new Blob(filename, CWD).getId();
                String sId = stage.getAdded().getOrDefault(filename, "");
                String hId = head.getBlobs().getOrDefault(filename, "");
                if ((hId != "" && hId != bId && sId == "") ||
                    (sId != "" && sId != bId)){
                    res.add(filename + " (modified)");
                }
            }
        }

        Collections.sort(res);
        return res;
    }

    /**
     * java gitlet.Main checkout -- [file name]
     */
    public void checkoutFileFromHead(String filename) {
        Commit head = getHead();
        checkoutFileFromCommit(head, filename);
    }

    /**
     * java gitlet.Main checkout [commit id] -- [file name]
     *
     * @return
     */
    public void checkoutFileFromCommitId(String commitId, String filename) {
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
    private String getCompleteCommitId(String commitId) {
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

    private void checkoutFileFromCommit(Commit commit, String filename) {
        String blobId = commit.getBlobs().getOrDefault(filename, "");
        checkoutFileFromBlobId(blobId);
    }

    private void checkoutFileFromBlobId(String blobId) {
        if (blobId.equals("")) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Blob blob = getBlobFromId(blobId);
        checkoutFileFromBlob(blob);
    }

    private void checkoutFileFromBlob(Blob blob) {
        File file = join(CWD, blob.getFilename());
        writeContents(file, blob.getContent());
    }

    /**
     * java gitlet.Main checkout [branch name]
     */
    public void checkoutBranch(String branchName) {
        File branchFile = getBranchFile(branchName);
        if (branchFile == null || !branchFile.exists()) {
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

    private Commit getCommitFromBranchName(String branchName) {
        File file = getBranchFile(branchName);
        return getCommitFromBranchFile(file);
    }

    /**
     * [branch]
     * [R1/branch]
     */
    private File getBranchFile(String branchName) {
        File file = null;
        String[] branches = branchName.split("/");
        if (branches.length == 1) {
            file = join(HEADS_DIR, branchName);
        } else if (branches.length == 2) {
            file = join(REMOTES_DIR, branches[0], branches[1]);
        }
        return file;
    }


    /**
     * java gitlet.Main branch [branch name]
     * create a new branch with name [branchName]
     * This command does NOT immediately switch to the newly created branch
     * (just as in real Git).
     */
    public void branch(String branchName) {
        File branch = join(HEADS_DIR, branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String commitId = getHeadCommitId();
        writeContents(branch, commitId);
    }

    private String getHeadCommitId() {
        String branchName = getHeadBranchName();
        File file = getBranchFile(branchName);
        return readContentsAsString(file);
    }

    /**
     * java gitlet.Main rm-branch [branch name]
     */
    public void rmBranch(String branchName) {
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

    public void reset(String commitId) {
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
    public void merge(String otherBranchName) {
        // failure cases:
        Stage stage = readStage();
        if (!stage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        File otherBranchFile = getBranchFile(otherBranchName);
        if (!otherBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(otherBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit head = getCommitFromBranchName(headBranchName);
        Commit other = getCommitFromBranchFile(otherBranchFile);
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
     * add-remote [remote name] [name of remote directory]/.gitlet
     */
    public void addRemote(String remoteName, String remotePath) {
        File remote = join(REMOTES_DIR, remoteName);
        if (remote.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        remote.mkdir();

        // java.io.File.separator
        if (File.separator.equals("\\")) {
            remotePath = remotePath.replaceAll("/", "\\\\\\\\");
        }

        /*
        [remote "origin"]
	        url = ..\\remotegit\\.git
	        fetch = +refs/heads/*:refs/remotes/origin/*
         */
        String content = readContentsAsString(CONFIG);
        content += "[remote \"" + remoteName + "\"]\n";
        content += remotePath + "\n";

        writeContents(CONFIG, content);
    }

    /**
     * java gitlet.Main rm-remote [remote name]
     */
    public void rmRemote(String remoteName) {
        File remote = join(REMOTES_DIR, remoteName);
        if (!remote.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }

        delFile(remote);

        String[] contents = readContentsAsString(CONFIG).split("\n");
        String target = "[remote \"" + remoteName + "\"]";;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < contents.length;) {
            if (contents[i].equals(target)) {
                i += 2;
            } else {
                sb.append(contents[i]);
            }
        }
        writeContents(CONFIG, sb.toString());
    }

    /**
     * java gitlet.Main push [remote name] [remote branch name]
     */
    public void push(String remoteName, String remoteBranchName) {
        File remotePath = getRemotePath(remoteName);
        Repository remote = new Repository(remotePath.getParent());

        Commit head = getHead();
        List<String> history = getHistory(head);
        Commit remoteHead = remote.getHead();
        if (!history.contains(remoteHead.getID())) {
            System.out.println("Please pull down remote changes before pushing.");
            System.exit(0);
        }

        // If the Gitlet system on the remote machine exists
        // but does not have the input branch,
        // then simply add the branch to the remote Gitlet.
        File remoteBranch = join(remote.HEADS_DIR, remoteBranchName);
        if (!remoteBranch.exists()) {
            remote.branch(remoteBranchName);
        }

        // append the future commits to the remote branch.
        for (String commitId : history) {
            if (commitId.equals(remoteHead.getID())) {
                break;
            }
            Commit commit = getCommitFromId(commitId);
            File remoteCommit = join(remote.COMMITS_DIR, commitId);
            writeObject(remoteCommit, commit);

            if (!commit.getBlobs().isEmpty()) {
                for (Map.Entry<String, String> item: commit.getBlobs().entrySet()) {
                    String blobId = item.getValue();
                    Blob blob = getBlobFromId(blobId);

                    File remoteBlob = join(remote.BLOBS_DIR, blobId);
                    writeObject(remoteBlob, blob);
                }
            }
        }

        // Then, the remote should reset to the front of the appended commits
        // (so its head will be the same as the local head).
        remote.reset(head.getID());
    }

    private List<String> getHistory(Commit head) {
        List<String> res = new LinkedList<>();
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

    private File getRemotePath(String remoteName) {
        String path = "";
        String[] contents = readContentsAsString(CONFIG).split("\n");
        for (int i = 0; i < contents.length;) {
            if (contents[i].contains(remoteName)) {
                path = contents[i + 1];
                break;
            } else {
                i += 2;
            }
        }

        File file = null;
        try {
            file = new File(path).getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (path.equals("") || !file.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        return file;
    }

    /**
     * java gitlet.Main fetch [remote name] [remote branch name]
     */
    public void fetch(String remoteName, String remoteBranchName) {
        File remotePath = getRemotePath(remoteName);

        Repository remote = new Repository(remotePath.getParent());

        File remoteBranchFile = remote.getBranchFile(remoteBranchName);
        if (remoteBranchFile == null || !remoteBranchFile.exists()) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }

        Commit remoteBranchCommit = remote.getCommitFromBranchFile(remoteBranchFile);

        // This branch is created in the local repository if it did not previously exist.
        // just update remotes/[remote]/[remote branch] file to new Commit id.
        File branch = join(REMOTES_DIR, remoteName, remoteBranchName);
        writeContents(branch, remoteBranchCommit.getID());

        // fetch down all commits and blobs
        List<String> history = remote.getHistory(remoteBranchCommit);

        for (String commitId : history) {
            Commit commit = remote.getCommitFromId(commitId);
            File commitFile = join(COMMITS_DIR, commit.getID());
            if (commitFile.exists()) {
                continue;
            }
            writeObject(commitFile, commit);

            if (commit.getBlobs().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> item: commit.getBlobs().entrySet()) {
                String blobId = item.getValue();
                Blob blob = remote.getBlobFromId(blobId);

                File blobFile = join(BLOBS_DIR, blobId);
                writeObject(blobFile, blob);
            }
        }
    }

    /**
     * java gitlet.Main pull [remote name] [remote branch name]
     */
    public void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);

        merge(remoteName + "/" + remoteBranchName);
    }



    /**
     * Helper Functions
     */

    private void commitWith(String message, List<Commit> parents) {
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
        String branchName = getHeadBranchName();
        File branch = getBranchFile(branchName);
        writeContents(branch, commitId);
    }



    // The split point is a latest common ancestor of the current and given branch heads
    private Commit getLatestCommonAncestor(Commit head, Commit other) {
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

    private Set<String> bfsFromCommit(Commit head) {
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

    private void mergeWithLCA(Commit lca, Commit head, Commit other) {
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

    private String getContentAsStringFromBlobId(String blobId) {
        if (blobId.equals("")) {
            return "";
        }
        return getBlobFromId(blobId).getContentAsString();
    }


    private String getConflictFile(String[] head, String[] other) {
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

    private String getConflictContent(String head, String other) {
        StringBuffer sb = new StringBuffer();
        sb.append("<<<<<<< HEAD\n");
        // contents of file in current branch
        sb.append(head.equals("") ? head : head + "\n");
        sb.append("=======\n");
        // contents of file in given branch
        sb.append(other.equals("") ? other : other + "\n");
        sb.append(">>>>>>>\n");
        return sb.toString();
    }

    private void rewriteFile(String filename, String content) {
        File file = join(CWD, filename);
        writeContents(file, content);
    }

    /**
     * be sure that blob id is not "".
     */
    private Blob getBlobFromId(String blobId) {
        File file = join(BLOBS_DIR, blobId);
        return readObject(file, Blob.class);
    }

    private Set<String> getAllFilenames(Commit lca, Commit head, Commit other) {
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
    private void validUntrackedFile(Map<String, String> blobs) {
        List<String> untrackedFiles = getUntrackedFiles();
        if (untrackedFiles.isEmpty()) {
            return;
        }

        for (String filename : untrackedFiles) {
            String blobId = new Blob(filename, CWD).getId();
            String otherId = blobs.getOrDefault(filename, "");
            if (!otherId.equals(blobId)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    private List<String> getUntrackedFiles() {
        List<String> res = new ArrayList<>();
        List<String> stageFiles = readStage().getStagedFilename();
        Set<String> headFiles = getHead().getBlobs().keySet();
        for (String filename : plainFilenamesIn(CWD)) {
            if (!stageFiles.contains(filename) && !headFiles.contains(filename)) {
                res.add(filename);
            }
        }
        Collections.sort(res);
        return res;
    }

    private void replaceWorkingPlaceWithCommit(Commit commit) {
        clearWorkingSpace();

        for (Map.Entry<String, String> item : commit.getBlobs().entrySet()) {
            String filename = item.getKey();
            String blobId = item.getValue();
            File file = join(CWD, filename);
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);

            writeContents(file, blob.getContent());
        }
    }

    private void clearWorkingSpace() {
        File[] files = CWD.listFiles(gitletFliter);
        for (File file : files) {
            delFile(file);
        }
    }

    private void delFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delFile(f);
            }
        }
        file.delete();
    }

    private FilenameFilter gitletFliter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(".gitlet");
        }
    };

    private List<File> getWorkingSpaceFiles() {
        File[] files = CWD.listFiles(gitletFliter);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /**
     * moving all staging dir's blob file to blobs dir.
     *
     * @param stage
     */
    private void clearStage(Stage stage) {
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

    private Stage readStage() {
        return readObject(STAGE, Stage.class);
    }

    private void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    private String getHeadBranchName() {
        return readContentsAsString(HEAD);
    }

    private String getCommitIdFromBranchFile(File file) {
        return readContentsAsString(file);
    }

    private Commit getCommitFromId(String commitId) {
        File file = join(COMMITS_DIR, commitId);
        if (commitId.equals("null") || !file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    private Commit getCommitFromBranchFile(File file) {
        String commitId = readContentsAsString(file);
        return getCommitFromId(commitId);
    }


    private Commit getHead() {
        String branchName = getHeadBranchName();
        File branchFile = getBranchFile(branchName);
        Commit head = getCommitFromBranchFile(branchFile);

        if (head == null) {
            System.out.println("error! cannot find HEAD!");
            System.exit(0);
        }

        return head;
    }

    private void writeCommitToFile(Commit commit) {
        File file = join(COMMITS_DIR, commit.getID());
        writeObject(file, commit);
    }


    /**
     * check things
     */
    void checkIfInitDirectoryExists() {
        if (!GITLET_DIR.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    void checkCommandLength(int actual, int expected) {
        if (actual != expected) {
            messageIncorrectOperands();
        }
    }

    void checkEqual(String actual, String expected) {
        if (!actual.equals(expected)) {
            messageIncorrectOperands();
        }
    }

    void messageIncorrectOperands() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }
}
