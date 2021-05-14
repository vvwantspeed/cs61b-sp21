package gitlet;

import java.io.File;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author vv
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // handle the `init` command
                Repository.checkCommandLength(args.length, 1);
                Repository.init();
                break;
            case "add":
                // handle the `add [filename]` command
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.add(args[1]);
                break;
            case "rm":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.rm(args[1]);
                break;
            case "commit":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.commit(args[1]);
                break;
            case "log":
                Repository.checkCommandLength(args.length, 1);
                Repository.checkIfInitDirectoryExists();
                Repository.log();
                break;
            case "global-log":
                Repository.checkCommandLength(args.length, 1);
                Repository.checkIfInitDirectoryExists();
                Repository.global_log();
                break;
            case "find":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.find(args[1]);
                break;
            case "status":
                Repository.checkCommandLength(args.length, 1);
                Repository.checkIfInitDirectoryExists();
                Repository.status();
                break;
            case "checkout":
                int len = args.length;  // 2 3 4
                if (len < 2 || len > 4) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.checkIfInitDirectoryExists();
                if (len == 2) {
                    // java gitlet.Main checkout [branch name]
                    Repository.checkoutBranch(args[1]);
                } else if (len == 3) {
                    // java gitlet.Main checkout -- [file name]
                    Repository.checkoutFileFromHead(args[2]);
                } else if (len == 4) {
                    // java gitlet.Main checkout [commit id] -- [file name]
                    Repository.checkoutFileFromCommitId(args[1], args[3]);
                }
                break;
            case "branch":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                Repository.checkCommandLength(args.length, 2);
                Repository.checkIfInitDirectoryExists();
                Repository.reset(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }


}
