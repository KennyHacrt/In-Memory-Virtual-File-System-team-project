package hk.edu.polyu.comp.comp2021.cvfs;

import hk.edu.polyu.comp.comp2021.cvfs.model.CVFS;
import static hk.edu.polyu.comp.comp2021.cvfs.model.CVFS.*;

import java.util.Scanner;

/**
 *
 */
public class Application {
    /**
     * Main method to start the CVFS application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args){
        CVFS cvfs = new CVFS();
        // Initialize and utilize the system
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Custom Virtual File System (CVFS)");
        while (true) {
            System.out.print("CVFS> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            String[] tokens = parseInput(input);
            String command = tokens[0];
            try {
                switch (command) {
                    case "newDisk":
                        newDisk(tokens);
                        break;
                    case "newDoc":
                        newDoc(tokens);
                        break;
                    case "newDir":
                        newDir(tokens);
                        break;
                    case "delete":
                        delete(tokens);
                        break;
                    case "rename":
                        rename(tokens);
                        break;
                    case "changeDir":
                        changeDir(tokens);
                        break;
                    case "list":
                        list();
                        break;
                    case "rList":
                        rList();
                        break;
                    case "newSimpleCri":
                        newSimpleCri(tokens);
                        break;
                    case "newNegation":
                        newNegation(tokens);
                        break;
                    case "newBinaryCri":
                        newBinaryCri(tokens);
                        break;
                    case "printAllCriteria":
                        printAllCriteria();
                        break;
                    case "search":
                        search(tokens);
                        break;
                    case "rSearch":
                        rSearch(tokens);
                        break;
                    case "save":
                        save(tokens);
                        break;
                    case "load":
                        load(tokens);
                        break;
                    case "quit":
                        System.out.println("Terminating the CVFS system.");
                        System.exit(0);
                        break;
                    case "undo":
                        undo();
                        break;
                    case "redo":
                        redo();
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}

