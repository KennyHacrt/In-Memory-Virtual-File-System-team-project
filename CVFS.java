package hk.edu.polyu.comp.comp2021.cvfs.model;
import java.io.*;
import java.util.*;

/**
 * Custom Virtual File System (CVFS) class that manages virtual disks, directories, documents, and criteria.
 */
public class CVFS {
    /**
     * Constructs a new instance of the CVFS class.
     * This constructor initializes a new CVFS object with default settings.
     * It does not take any parameters and does not perform any additional actions
     * during initialization.
     */
    public CVFS(){}

    // Constants
    private static final int BASE_FILE_SIZE = 40;
    private static final int MAX_FILE_NAME_LENGTH = 10;
    private static final Set<String> ALLOWED_DOC_TYPES = new HashSet<>(Arrays.asList("txt", "java", "html", "css"));
    private static final String NAME_PATTERN = "^[A-Za-z0-9]{1,10}$";
    private static final String CRI_NAME_PATTERN = "^[A-Za-z]{2}$";

    // Global variables
    static VirtualDisk workingDisk = null;
    static Directory workingDirectory = null;
    static Map<String, Criterion> criteriaMap = new HashMap<>();
    static Stack<Command> undoStack = new Stack<>();
    static Stack<Command> redoStack = new Stack<>();

    // Main method to run the CLI tool


    // Parses the input line into tokens, considering quoted strings
    /**
     * Parses the input string into an array of tokens, handling quoted phrases.
     * <p>
     * This method splits the input string by whitespace while respecting quotes.
     * If a segment of the input is enclosed in double quotes, it is treated as a single token.
     * For example, the input {@code "hello world" foo bar} will result in
     * an array containing {@code ["hello world", "foo", "bar"]}.
     * </p>
     *
     * @param input the input string to be parsed
     * @return an array of tokens extracted from the input string
     */
    public static String[] parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                // Do not append the quote character
            } else if (Character.isWhitespace(c) && !inQuote) {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb = new StringBuilder();
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    // REQ1: newDisk command
    /**
     * Creates a new virtual disk with the specified size.
     * <p>
     * This method initializes a new instance of {@link VirtualDisk}
     * with the size provided in the command tokens. It clears the
     * current state, including the criteria map and undo/redo stacks.
     * The method expects the command tokens to contain exactly two elements:
     * the command itself and the desired disk size.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] must represent the disk size
     * @throws Exception if the number of tokens is not equal to 2 or
     *                   if the size cannot be parsed as an integer
     */
    public static void newDisk(String[] tokens) throws Exception {
        if (tokens.length != 2) throw new Exception("Usage: newDisk diskSize");
        int size = Integer.parseInt(tokens[1]);
        if (size < 0) throw new Exception("Error: Disk size should not be negative.");
        workingDisk = new VirtualDisk(size);
        workingDirectory = workingDisk.getRootDirectory();
        criteriaMap.clear(); // Clear existing criteria

        // Re-add the IsDocument criterion
        criteriaMap.put("IsDocument", new IsDocumentCriterion());

        undoStack.clear();
        redoStack.clear();
        System.out.println("New virtual disk created with size " + size);
    }
    // REQ2: newDoc command
    /**
     * Creates a new document in the currently loaded virtual disk.
     * <p>
     * This method adds a new {@link Document} to the working directory
     * of the virtual disk. It validates the number of tokens, ensures
     * that a virtual disk is loaded, and checks if the provided document
     * type is allowed. The method expects four tokens: the command,
     * document name, document type, and document content.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the document name,
     *               tokens[2] is the document type, and
     *               tokens[3] is the document content
     * @throws Exception if no virtual disk is loaded, if the number of
     *                   tokens is not equal to 4, or if the document
     *                   type is not allowed
     */
    public static void newDoc(String[] tokens) throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        if (tokens.length != 4) throw new Exception("Usage: newDoc docName docType docContent");
        String name = tokens[1];
        String type = tokens[2];
        String content = tokens[3];
        if (!ALLOWED_DOC_TYPES.contains(type)) throw new Exception("Document type not allowed.");
        Document doc = new Document(name, type, content);
        workingDirectory.addFile(doc);
        workingDisk.updateSize();
        undoStack.push(new NewDocCommand(doc, workingDirectory));
        redoStack.clear();
        System.out.println("Document " + name + " created.");
    }

    // REQ3: newDir command
    /**
     * Creates a new directory in the currently loaded virtual disk.
     * <p>
     * This method adds a new {@link Directory} to the working directory
     * of the virtual disk. It checks if a virtual disk is loaded and
     * validates that the correct number of tokens is provided. The method
     * expects two tokens: the command and the directory name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the directory name
     * @throws Exception if no virtual disk is loaded, or if the number
     *                   of tokens is not equal to 2
     */
    public static void newDir(String[] tokens) throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        if (tokens.length != 2) throw new Exception("Usage: newDir dirName");
        String name = tokens[1];
        Directory dir = new Directory(name, workingDirectory);
        workingDirectory.addFile(dir);
        workingDisk.updateSize();
        undoStack.push(new NewDirCommand(dir, workingDirectory));
        redoStack.clear();
        System.out.println("Directory " + name + " created.");
    }

    // REQ4: delete command
    /**
     * Deletes a file from the currently loaded virtual disk.
     * <p>
     * This method removes a specified {@link File} from the working
     * directory of the virtual disk. It checks if a virtual disk is
     * loaded and validates that the correct number of tokens is provided.
     * The method expects two tokens: the command and the file name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the name of the file to be deleted
     * @throws Exception if no virtual disk is loaded, if the number
     *                   of tokens is not equal to 2, or if the file
     *                   specified cannot be found in the working directory
     */
    public static void delete(String[] tokens) throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        if (tokens.length != 2) throw new Exception("Usage: delete fileName");
        String name = tokens[1];
        File file = workingDirectory.getFile(name);
        if (file == null) throw new Exception("File not found.");
        workingDirectory.removeFile(name);
        workingDisk.updateSize();
        undoStack.push(new DeleteCommand(file, workingDirectory));
        redoStack.clear();
        System.out.println("File " + name + " deleted.");
    }

    // REQ5: rename command
    /**
     * Renames an existing file in the currently loaded virtual disk.
     * <p>
     * This method changes the name of a specified {@link File}
     * in the working directory. It checks if a virtual disk is loaded,
     * validates the number of tokens, and ensures that the new file name
     * is valid and does not already exist. The method expects three tokens:
     * the command, the old file name, and the new file name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the old file name and
     *               tokens[2] is the new file name
     * @throws Exception if no virtual disk is loaded, if the number
     *                   of tokens is not equal to 3, if the old file
     *                   is not found, if the new file name is invalid,
     *                   or if a file with the new name already exists
     */
    public static void rename(String[] tokens) throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        if (tokens.length != 3) throw new Exception("Usage: rename oldFileName newFileName");
        String oldName = tokens[1];
        String newName = tokens[2];
        File file = workingDirectory.getFile(oldName);
        if (file == null) throw new Exception("File not found.");
        if (!isValidName(newName)) throw new Exception("Invalid new file name.");
        if (workingDirectory.getFile(newName) != null) throw new Exception("A file with the new name already exists.");
        String prevName = file.getName();
        file.setName(newName);
        undoStack.push(new RenameCommand(file, prevName));
        redoStack.clear();
        System.out.println("File " + oldName + " renamed to " + newName);
    }

    // REQ6: changeDir command
    /**
     * Changes the current working directory in the virtual disk.
     * <p>
     * This method updates the current working directory to a
     * specified {@link Directory}. It supports navigating to the
     * parent directory using the ".." command. The method expects
     * two tokens: the command and the directory name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the directory name or ".."
     * @throws Exception if no virtual disk is loaded, if the number
     *                   of tokens is not equal to 2, if attempting
     *                   to change to a non-existent directory, or if
     *                   already at the root directory
     */

    public static void changeDir(String[] tokens) throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        if (tokens.length != 2) throw new Exception("Usage: changeDir dirName");
        String name = tokens[1];
        Directory prevDir = workingDirectory;
        if (name.equals("..")) {
            if (workingDirectory.getParent() != null) {
                workingDirectory = workingDirectory.getParent();
                undoStack.push(new ChangeDirCommand(prevDir));
                redoStack.clear();
                System.out.println("Changed to parent directory.");
            } else {
                throw new Exception("Already at the root directory.");
            }
        } else {
            File file = workingDirectory.getFile(name);
            if (file instanceof Directory) {
                workingDirectory = (Directory) file;
                undoStack.push(new ChangeDirCommand(prevDir));
                redoStack.clear();
                System.out.println("Changed to directory " + name);
            } else {
                throw new Exception("Directory not found.");
            }
        }
    }

    // REQ7: list command
    /**
     * Lists all files and directories in the current working directory.
     * <p>
     * This method retrieves and displays all {@link File} objects in
     * the working directory. It outputs the name, type, and size of
     * each document and directory. It also displays the total count of
     * files and their cumulative size.
     * </p>
     *
     * @throws Exception if no virtual disk is loaded
     */
    public static void list() throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        List<File> files = workingDirectory.getFiles();
        int totalSize = 0;
        int count = 0;
        for (File file : files) {
            if (file instanceof Document) {
                Document doc = (Document) file;
                System.out.println("Document Name: " + doc.getName() + ", Type: " + doc.getType() + ", Size: " + doc.getSize());
            } else if (file instanceof Directory) {
                System.out.println("Directory Name: " + file.getName() + ", Size: " + file.getSize());
            }
            totalSize += file.getSize();
            count++;
        }
        System.out.println("Total files: " + count + ", Total size: " + totalSize);
    }

    // REQ8: rList command
    /**
     * Recursively lists all files and directories in the current working directory and its subdirectories.
     * <p>
     * This method initiates a recursive listing of all {@link File} objects
     * in the working directory and its child directories. It outputs the
     * name, type, and size of each document and directory, and displays
     * the total count of files and their cumulative size.
     * </p>
     *
     * @throws Exception if no virtual disk is loaded
     */
    public static void rList() throws Exception {
        if (workingDisk == null) throw new Exception("No virtual disk loaded.");
        int[] total = new int[2]; // total[0]: count, total[1]: size
        recursiveList(workingDirectory, 0, total);
        System.out.println("Total files: " + total[0] + ", Total size: " + total[1]);
    }

    /**
     * Recursively lists all files and directories in the specified directory.
     * <p>
     * This helper method traverses the provided {@link Directory} and
     * outputs the name, type, and size of each document and directory.
     * It also accumulates the total count of files and their cumulative size.
     * </p>
     *
     * @param dir the directory to list files from
     * @param level the current depth level for indentation
     * @param total an array where total[0] is the count of files and
     *               total[1] is the cumulative size of files
     */
    public static void recursiveList(Directory dir, int level, int[] total) {
        String indent = generateIndent(level);
        for (File file : dir.getFiles()) {
            if (file instanceof Document) {
                Document doc = (Document) file;
                System.out.println(indent + "Document Name: " + doc.getName() + ", Type: " + doc.getType() + ", Size: " + doc.getSize());
            } else if (file instanceof Directory) {
                System.out.println(indent + "Directory Name: " + file.getName() + ", Size: " + file.getSize());
                recursiveList((Directory) file, level + 1, total);
            }
            total[0]++;
            total[1] += file.getSize();
        }
    }

    // Helper method to generate indentation string
    /**
     * Generates an indentation string based on the specified level.
     * <p>
     * This helper method creates a string containing spaces for indentation,
     * which is used to format the output of the directory listing based on
     * the depth of the directory structure.
     * </p>
     *
     * @param level the depth level for which to generate indentation
     * @return a string representing the indentation
     */
    public static String generateIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    // REQ9: newSimpleCri command
    /**
     * Creates a new simple criterion and adds it to the criteria map.
     * <p>
     * This method initializes a {@link SimpleCriterion} with the specified
     * attribute name, operator, and value. It validates the inputs, ensuring
     * that the correct number of tokens are provided, that the criterion name
     * follows the defined pattern, and that it does not already exist in the
     * criteria map. The method expects five tokens: the command, criterion name,
     * attribute name, operator, and value.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the criterion name,
     *               tokens[2] is the attribute name,
     *               tokens[3] is the operator, and
     *               tokens[4] is the value
     * @throws Exception if the number of tokens is not equal to 5,
     *                   if the criterion name is invalid, or if
     *                   a criterion with the same name already exists
     */
    public static void newSimpleCri(String[] tokens) throws Exception {
        if (tokens.length != 5) throw new Exception("Usage: newSimpleCri criName attrName op val");
        String criName = tokens[1];
        String attrName = tokens[2];
        String op = tokens[3];
        String val = tokens[4];
        if (!criName.matches(CRI_NAME_PATTERN)) throw new Exception("Invalid criterion name.");
        if (criteriaMap.containsKey(criName)) throw new Exception("Criterion name already exists.");
        Criterion criterion = new SimpleCriterion(attrName, op, val);
        criteriaMap.put(criName, criterion);
        undoStack.push(new NewCriterionCommand(criName));
        redoStack.clear();
        System.out.println("Simple criterion " + criName + " created.");
    }

    // REQ10: IsDocument criterion
    static {
        criteriaMap.put("IsDocument", new IsDocumentCriterion());
    }

    // REQ11: newNegation and newBinaryCri commands
    /**
     * Creates a new negation criterion based on an existing criterion.
     * <p>
     * This method initializes a {@link NegationCriterion} using the
     * specified existing criterion. It validates the inputs, ensuring
     * that the correct number of tokens are provided, that the criterion
     * name follows the defined pattern, and that the referenced criterion
     * exists. The method expects three tokens: the command, the name of
     * the new negation criterion, and the name of the existing criterion.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the new criterion name and
     *               tokens[2] is the existing criterion name
     * @throws Exception if the number of tokens is not equal to 3,
     *                   if the new criterion name is invalid,
     *                   if the new criterion name already exists,
     *                   or if the referenced criterion does not exist
     */
    public static void newNegation(String[] tokens) throws Exception {
        if (tokens.length != 3) throw new Exception("Usage: newNegation criName1 criName2");
        String criName1 = tokens[1];
        String criName2 = tokens[2];
        if (!criName1.matches(CRI_NAME_PATTERN)) throw new Exception("Invalid criterion name.");
        if (criteriaMap.containsKey(criName1)) throw new Exception("Criterion name already exists.");
        Criterion c2 = criteriaMap.get(criName2);
        if (c2 == null) throw new Exception("Criterion " + criName2 + " does not exist.");
        Criterion criterion = new NegationCriterion(c2);
        criteriaMap.put(criName1, criterion);
        undoStack.push(new NewCriterionCommand(criName1));
        redoStack.clear();
        System.out.println("Negation criterion " + criName1 + " created.");
    }

    /**
     * Creates a new binary criterion based on two existing criteria.
     * <p>
     * This method initializes a {@link BinaryCriterion} using the specified
     * existing criteria and a logical operator. It validates the inputs,
     * ensuring that the correct number of tokens are provided, that the
     * new criterion name follows the defined pattern, and that both
     * referenced criteria exist. The method expects five tokens: the
     * command, the name of the new binary criterion, the name of the
     * first existing criterion, the logical operator, and the name of the
     * second existing criterion.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the new criterion name,
     *               tokens[2] is the first existing criterion name,
     *               tokens[3] is the logical operator, and
     *               tokens[4] is the second existing criterion name
     * @throws Exception if the number of tokens is not equal to 5,
     *                   if the new criterion name is invalid,
     *                   if the new criterion name already exists,
     *                   or if either of the referenced criteria do not exist
     */
    public static void newBinaryCri(String[] tokens) throws Exception {
        if (tokens.length != 5) throw new Exception("Usage: newBinaryCri criName1 criName3 logicOp criName4");
        String criName1 = tokens[1];
        String criName3 = tokens[2];
        String logicOp = tokens[3];
        String criName4 = tokens[4];
        if (!criName1.matches(CRI_NAME_PATTERN)) throw new Exception("Invalid criterion name.");
        if (criteriaMap.containsKey(criName1)) throw new Exception("Criterion name already exists.");
        Criterion c3 = criteriaMap.get(criName3);
        Criterion c4 = criteriaMap.get(criName4);
        if (c3 == null || c4 == null) throw new Exception("Criteria do not exist.");
        Criterion criterion = new BinaryCriterion(c3, logicOp, c4);
        criteriaMap.put(criName1, criterion);
        undoStack.push(new NewCriterionCommand(criName1));
        redoStack.clear();
        System.out.println("Binary criterion " + criName1 + " created.");
    }

    // REQ12: printAllCriteria command
    /**
     * Prints all criteria stored in the criteria map.
     * <p>
     * This method iterates through all entries in the criteria map and
     * prints each criterion's name along with its string representation.
     * It provides a convenient way to view all defined criteria in the
     * system.
     * </p>
     */
    public static void printAllCriteria() {
        for (Map.Entry<String, Criterion> entry : criteriaMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue().toString());
        }
    }

    // REQ13: search command
    /**
     * Searches for files in the current working directory that match the specified criterion.
     * <p>
     * This method evaluates the files in the working directory against the
     * given criterion. It prints the details of each matching file, including
     * the name, type, and size. The method expects two tokens: the command and
     * the criterion name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the name of the criterion to use for the search
     * @throws Exception if the number of tokens is not equal to 2,
     *                   or if the specified criterion is not found
     */
    public static void search(String[] tokens) throws Exception {
        if (tokens.length != 2) throw new Exception("Usage: search criName");
        Criterion criterion = criteriaMap.get(tokens[1]);
        if (criterion == null) throw new Exception("Criterion not found.");
        List<File> files = workingDirectory.getFiles();
        int totalSize = 0;
        int count = 0;
        for (File file : files) {
            if (criterion.evaluate(file)) {
                if (file instanceof Document) {
                    Document doc = (Document) file;
                    System.out.println("Document Name: " + doc.getName() + ", Type: " + doc.getType() + ", Size: " + doc.getSize());
                } else if (file instanceof Directory) {
                    System.out.println("Directory Name: " + file.getName() + ", Size: " + file.getSize());
                }
                totalSize += file.getSize();
                count++;
            }
        }
        System.out.println("Total files: " + count + ", Total size: " + totalSize);
    }

    // REQ14: rSearch command
    /**
     * Recursively searches for files in the current working directory and its subdirectories
     * that match the specified criterion.
     * <p>
     * This method evaluates the files in the working directory and all subdirectories
     * against the given criterion. It prints the details of each matching file, including
     * the name, type, and size. The method expects two tokens: the command and the
     * criterion name.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the name of the criterion to use for the search
     * @throws Exception if the number of tokens is not equal to 2,
     *                   or if the specified criterion is not found
     */
    public static void rSearch(String[] tokens) throws Exception {
        if (tokens.length != 2) throw new Exception("Usage: rSearch criName");
        Criterion criterion = criteriaMap.get(tokens[1]);
        if (criterion == null) throw new Exception("Criterion not found.");
        int[] total = new int[2]; // total[0]: count, total[1]: size
        recursiveSearch(workingDirectory, criterion, total);
        System.out.println("Total files: " + total[0] + ", Total size: " + total[1]);
    }
    /**
     * Recursively searches for files in the specified directory that match the given criterion.
     * <p>
     * This method traverses the provided {@link Directory} and evaluates each file
     * against the specified criterion. It prints the details of each matching file,
     * including the name, type, and size, while accumulating the total count and size
     * of the matching files.
     * </p>
     *
     * @param dir the directory to search within
     * @param criterion the criterion used to evaluate files
     * @param total an array where total[0] is the count of matching files and
     *               total[1] is their cumulative size
     */
    public static void recursiveSearch(Directory dir, Criterion criterion, int[] total) {
        for (File file : dir.getFiles()) {
            if (criterion.evaluate(file)) {
                if (file instanceof Document) {
                    Document doc = (Document) file;
                    System.out.println("Document Name: " + doc.getName() + ", Type: " + doc.getType() + ", Size: " + doc.getSize());
                } else if (file instanceof Directory) {
                    System.out.println("Directory Name: " + file.getName() + ", Size: " + file.getSize());
                }
                total[0]++;
                total[1] += file.getSize();
            }
            if (file instanceof Directory) {
                recursiveSearch((Directory) file, criterion, total);
            }
        }
    }

    // REQ15: save command
    /**
     * Saves the current state of the virtual disk and criteria map to a specified file.
     * <p>
     * This method serializes the current {@link VirtualDisk} and the criteria map to
     * a file specified by the provided path. It expects two tokens: the command and
     * the path where the data should be saved. If the save operation fails, an exception
     * is thrown with an appropriate error message.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the file path to save the virtual disk
     * @throws Exception if the number of tokens is not equal to 2,
     *                   or if an I/O error occurs during saving
     */
    public static void save(String[] tokens) throws Exception {
        if (tokens.length != 2) throw new Exception("Usage: save path");
        String path = tokens[1];
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(workingDisk);
            oos.writeObject(criteriaMap);
            System.out.println("Virtual disk saved to " + path);
        } catch (IOException e) {
            throw new Exception("Failed to save virtual disk: " + e.getMessage());
        }
    }

    // REQ16: load command
    /**
     * Loads the state of the virtual disk and criteria map from a specified file.
     * <p>
     * This method deserializes the {@link VirtualDisk} and the criteria map from
     * a file specified by the provided path. It expects two tokens: the command
     * and the path from which to load the data. If the load operation fails,
     * an exception is thrown with an appropriate error message.
     * </p>
     *
     * @param tokens an array of strings containing command tokens,
     *               where tokens[1] is the file path to load the virtual disk from
     * @throws Exception if the number of tokens is not equal to 2,
     *                   or if an I/O error or class not found error occurs during loading
     */
    @SuppressWarnings("unchecked")
    public static void load(String[] tokens) throws Exception {
        if (tokens.length != 2) throw new Exception("Usage: load path");
        String path = tokens[1];
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            workingDisk = (VirtualDisk) ois.readObject();
            criteriaMap = (Map<String, Criterion>) ois.readObject();
            workingDirectory = workingDisk.getRootDirectory();
            undoStack.clear();
            redoStack.clear();
            System.out.println("Virtual disk loaded from " + path);
        } catch (IOException | ClassNotFoundException e) {
            throw new Exception("Failed to load virtual disk: " + e.getMessage());
        }
    }

    // undo and redo commands
    /**
     * Reverses the most recent action performed in the system.
     * <p>
     * This method pops the last command from the undo stack and
     * invokes its {@code undo} method. The command is then pushed
     * onto the redo stack, allowing it to be reapplied later if needed.
     * If there are no commands to undo, an exception is thrown.
     * </p>
     *
     * @throws Exception if the undo stack is empty, indicating
     *                   that there is nothing to undo
     */
    public static void undo() throws Exception {
        if (undoStack.isEmpty()) throw new Exception("Nothing to undo.");
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        System.out.println("Undo successful.");
    }
    /**
     * Reapplies the most recently undone action in the system.
     * <p>
     * This method pops the last command from the redo stack and
     * invokes its {@code redo} method. The command is then pushed
     * back onto the undo stack, allowing it to be undone again if needed.
     * If there are no commands to redo, an exception is thrown.
     * </p>
     *
     * @throws Exception if the redo stack is empty, indicating
     *                   that there is nothing to redo
     */
    public static void redo() throws Exception {
        if (redoStack.isEmpty()) throw new Exception("Nothing to redo.");
        Command cmd = redoStack.pop();
        cmd.redo();
        undoStack.push(cmd);
        System.out.println("Redo successful.");
    }

    // Abstract File class
    /**
     * Abstract class representing a file in the system.
     * <p>
     * This class serves as a base for different types of files, providing
     * common functionality such as name validation and retrieval. It
     * includes an abstract method for getting the size of the file, which
     * must be implemented by subclasses. Each file has a name and a
     * reference to its parent directory.
     * </p>
     */
    static abstract class File implements Serializable {
        /**
         *
         */
        protected String name;
        /**
         *
         */
        public Directory parent;
        /**
         * Constructs a new File with the specified name.
         *
         * @param name the name of the file
         * @throws Exception if the file name is invalid
         */

        public File(String name) throws Exception {
            if (!isValidName(name)) throw new Exception("Invalid file name.");
            this.name = name;
        }
        /**
         * Returns the name of the file.
         *
         * @return the name of the file
         */
        public String getName() {
            return name;
        }
        /**
         * Sets the name of the file.
         *
         * @param name the new name of the file
         * @throws Exception if the new file name is invalid
         */
        public void setName(String name) throws Exception {
            if (!isValidName(name)) throw new Exception("Invalid file name.");
            this.name = name;
        }
        /**
         * Abstract method to get the size of the file.
         *
         * @return the size of the file in bytes
         */
        public abstract int getSize();
    }

    // Directory class
    /**
     * Represents a directory that can contain files and other directories.
     * <p>
     * This class extends the {@link File} class to provide functionality
     * for managing a collection of files. It includes methods for adding,
     * removing, and retrieving files, as well as calculating the total size
     * of the directory, including all its contents.
     * </p>
     */
    static class Directory extends File {
        private List<File> files;
        /**
         * Constructs a new Directory with the specified name and parent directory.
         *
         * @param name the name of the directory
         * @param parent the parent directory of the new directory
         * @throws Exception if the directory name is invalid
         */

        public Directory(String name, Directory parent) throws Exception {
            super(name);
            this.parent = parent;
            this.files = new ArrayList<>();
        }
        /**
         * Returns the parent directory of this directory.
         *
         * @return the parent directory
         */
        public Directory getParent() {
            return parent;
        }
        /**
         * Adds a file or directory to this directory.
         *
         * @param file the file or directory to add
         * @throws Exception if a file with the same name already exists
         */
        public void addFile(File file) throws Exception {
            if (files.stream().anyMatch(f -> f.getName().equals(file.getName())))
                throw new Exception("File with the same name already exists.");
            files.add(file);
            if (file instanceof Directory) ((Directory) file).parent = this;
        }
        /**
         * Removes a file or directory by name from this directory.
         *
         * @param name the name of the file to remove
         * @throws Exception if the file is not found
         */
        public void removeFile(String name) throws Exception {
            File toRemove = null;
            for (File f : files) {
                if (f.getName().equals(name)) {
                    toRemove = f;
                    break;
                }
            }
            if (toRemove != null) {
                files.remove(toRemove);
            } else {
                throw new Exception("File not found.");
            }
        }
        /**
         * Retrieves a file or directory by name from this directory.
         *
         * @param name the name of the file to retrieve
         * @return the file with the specified name, or null if not found
         */
        public File getFile(String name) {
            return files.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
        }
        /**
         * Returns the list of files in this directory.
         *
         * @return a list of files contained in this directory
         */
        public List<File> getFiles() {
            return files;
        }

        @Override
        public int getSize() {
            int totalSize = BASE_FILE_SIZE;
            for (File file : files) {
                totalSize += file.getSize();
            }
            return totalSize;
        }
    }

    // Document class
    /**
     * Represents a document file in the system.
     * <p>
     * This class extends the {@link File} class to provide functionality
     * specific to document files, including type validation and content management.
     * It includes methods for retrieving the document type and calculating the
     * size of the document based on its content.
     * </p>
     */

    static class Document extends File {
        private String type;
        private String content;
        /**
         * Constructs a new Document with the specified name, type, and content.
         *
         * @param name the name of the document
         * @param type the type of the document
         * @param content the content of the document
         * @throws Exception if the document name is invalid or the document type is not allowed
         */
        public Document(String name, String type, String content) throws Exception {
            super(name);
            if (!ALLOWED_DOC_TYPES.contains(type)) throw new Exception("Invalid document type.");
            this.type = type;
            this.content = content;
        }
        /**
         * Returns the type of the document.
         *
         * @return the document type
         */
        public String getType() {
            return type;
        }
        /**
         * Returns the length of the document's content.
         *
         * @return the length of the content in characters
         */
        public int getContentLength() {
            return content.length();
        }

        @Override
        public int getSize() {
            return BASE_FILE_SIZE + content.length() * 2;
        }
    }

    // VirtualDisk class
    /**
     * Represents a virtual disk that can hold files and directories.
     * <p>
     * This class encapsulates a virtual disk with a maximum size limit
     * and a root directory. It provides methods to access the root directory
     * and ensure that the disk size does not exceed the specified limit.
     * </p>
     */
    static class VirtualDisk implements Serializable {
        private int maxSize;
        private Directory rootDirectory;
        /**
         * Constructs a new VirtualDisk with a specified maximum size.
         *
         * @param maxSize the maximum size of the virtual disk in bytes
         * @throws Exception if the maximum size is less than or equal to zero
         */
        public VirtualDisk(int maxSize) throws Exception {
            this.maxSize = maxSize;
            this.rootDirectory = new Directory("root", null);
        }
        /**
         * Returns the root directory of the virtual disk.
         *
         * @return the root directory
         */
        public Directory getRootDirectory() {
            return rootDirectory;
        }
        /**
         * Updates the current size of the virtual disk and checks if it exceeds the maximum size.
         *
         * @throws Exception if the current size exceeds the maximum size of the disk
         */
        public void updateSize() throws Exception {
            int currentSize = rootDirectory.getSize();
            if (currentSize > maxSize) throw new Exception("Disk space exceeded.");
        }
    }

    // Criterion interfaces and classes
    /**
     * Interface representing a criterion for evaluating files.
     * <p>
     * This interface defines a method for evaluating a file based on
     * specific criteria. Implementations of this interface can provide
     * various ways to filter or select files based on attributes like
     * name, type, or size.
     * </p>
     */
    interface Criterion extends Serializable {
        /**
         * Evaluates whether the specified file meets the criterion.
         *
         * @param file the file to evaluate
         * @return true if the file meets the criterion, false otherwise
         */
        boolean evaluate(File file);

        String toString();
    }
    /**
     * Represents a simple criterion for evaluating files based on specific attributes.
     * <p>
     * This class implements the {@link Criterion} interface and allows
     * evaluation based on attributes such as name, type, and size.
     * It validates the attribute name, operator, and value upon creation.
     * </p>
     */
    static class SimpleCriterion implements Criterion {
        private String attrName;
        private String op;
        private String val;
        /**
         * Constructs a new SimpleCriterion with the specified attribute name, operator, and value.
         *
         * @param attrName the attribute name to evaluate (e.g., "name", "type", "size")
         * @param op the operator to use for evaluation (e.g., "contains", "equals", etc.)
         * @param val the value to compare against, formatted as required for the attribute
         * @throws Exception if the attribute name, operator, or value is invalid
         */
        public SimpleCriterion(String attrName, String op, String val) throws Exception {
            this.attrName = attrName;
            this.op = op;
            this.val = val;
            validate();
        }
        /**
         * Validates the attribute name, operator, and value for this criterion.
         *
         * @throws Exception if the attribute name, operator, or value fails validation
         */
        public void validate() throws Exception {
            switch (attrName) {
                case "name":
                    if (!op.equals("contains")) throw new Exception("Invalid operator for name attribute.");
                    // No need to check for double quotes
                    break;
                case "type":
                    if (!op.equals("equals")) throw new Exception("Invalid operator for type attribute.");
                    // No need to check for double quotes
                    break;
                case "size":
                    if (!op.matches(">|<|>=|<=|==|!=")) throw new Exception("Invalid operator for size attribute.");
                    try {
                        Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        throw new Exception("Value must be an integer.");
                    }
                    break;
                default:
                    throw new Exception("Invalid attribute name.");
            }
        }

        @Override
        public boolean evaluate(File file) {
            switch (attrName) {
                case "name":
                    return file.getName().contains(val);
                case "type":
                    if (file instanceof Document) {
                        return ((Document) file).getType().equals(val);
                    }
                    return false;
                case "size":
                    int sizeVal = Integer.parseInt(val);
                    int fileSize = file.getSize();
                    switch (op) {
                        case ">":
                            return fileSize > sizeVal;
                        case "<":
                            return fileSize < sizeVal;
                        case ">=":
                            return fileSize >= sizeVal;
                        case "<=":
                            return fileSize <= sizeVal;
                        case "==":
                            return fileSize == sizeVal;
                        case "!=":
                            return fileSize != sizeVal;
                    }
                    return false;
                default:
                    return false;
            }
        }

        @Override
        public String toString() {
            return "attrName: " + attrName + ", op: " + op + ", val: " + val;
        }
    }
    /**
     * Criterion that evaluates whether a file is a Document.
     * <p>
     * This class implements the {@link Criterion} interface and provides
     * a method to check if a given file is an instance of the {@link Document} class.
     * </p>
     */
    static class IsDocumentCriterion implements Criterion {
        @Override
        public boolean evaluate(File file) {
            return file instanceof Document;
        }

        @Override
        public String toString() {
            return "IsDocument";
        }
    }
    /**
     * Criterion that negates the evaluation of another criterion.
     * <p>
     * This class implements the {@link Criterion} interface and wraps another
     * criterion to provide a negated evaluation. It allows for logical
     * combinations of criteria.
     * </p>
     */
    static class NegationCriterion implements Criterion {
        private Criterion criterion;
        /**
         * Constructs a NegationCriterion that negates the specified criterion.
         *
         * @param criterion the criterion to negate
         */
        public NegationCriterion(Criterion criterion) {
            this.criterion = criterion;
        }

        @Override
        public boolean evaluate(File file) {
            return !criterion.evaluate(file);
        }

        @Override
        public String toString() {
            return "!( " + criterion.toString() + " )";
        }
    }
    /**
     * Criterion that combines two criteria using a logical operator.
     * <p>
     * This class implements the {@link Criterion} interface and allows for
     * the evaluation of two criteria using logical operations such as AND
     * (&&) and OR (||). It provides a way to create complex criteria by
     * combining simpler ones.
     * </p>
     */
    static class BinaryCriterion implements Criterion {
        private Criterion c1;
        private String logicOp;
        private Criterion c2;
        /**
         * Constructs a BinaryCriterion that combines two criteria with a specified logical operator.
         *
         * @param c1 the first criterion
         * @param logicOp the logical operator ("&&" for AND, "||" for OR)
         * @param c2 the second criterion
         * @throws Exception if the logical operator is invalid
         */
        public BinaryCriterion(Criterion c1, String logicOp, Criterion c2) throws Exception {
            if (!logicOp.equals("&&") && !logicOp.equals("||")) throw new Exception("Invalid logical operator.");
            this.c1 = c1;
            this.logicOp = logicOp;
            this.c2 = c2;
        }

        @Override
        public boolean evaluate(File file) {
            switch (logicOp) {
                case "&&":
                    return c1.evaluate(file) && c2.evaluate(file);
                case "||":
                    return c1.evaluate(file) || c2.evaluate(file);
                default:
                    return false;
            }
        }

        @Override
        public String toString() {
            return "( " + c1.toString() + " ) " + logicOp + " ( " + c2.toString() + " )";
        }
    }

    // Command interfaces and classes for undo/redo functionality
    /**
     * Interface representing a command for undo/redo functionality.
     * <p>
     * This interface defines methods for undoing and redoing operations.
     * Implementations of this interface encapsulate specific actions that
     * can be reversed or reapplied, facilitating an undo/redo mechanism.
     * </p>
     */
    interface Command {
        /**
         * Undoes the operation represented by this command.
         *
         * @throws Exception if an error occurs during the undo operation
         */
        void undo() throws Exception;
        /**
         * Redoes the operation represented by this command.
         *
         * @throws Exception if an error occurs during the redo operation
         */
        void redo() throws Exception;
    }
    /**
     * Command to create a new document in a specified directory.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the creation of a new document. It provides methods for undoing and
     * redoing the creation of the document, updating the directory and
     * the virtual disk size accordingly.
     * </p>
     */
    static class NewDocCommand implements Command {
        private Document doc;
        private Directory dir;
        /**
         * Constructs a NewDocCommand to create a new document in the specified directory.
         *
         * @param doc the document to create
         * @param dir the directory in which to create the document
         */
        public NewDocCommand(Document doc, Directory dir) {
            this.doc = doc;
            this.dir = dir;
        }

        @Override
        public void undo() throws Exception {
            dir.removeFile(doc.getName());
            workingDisk.updateSize();
        }

        @Override
        public void redo() throws Exception {
            dir.addFile(doc);
            workingDisk.updateSize();
        }
    }

    /**
     * Command to create a new directory in a specified directory.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the creation of a new directory. It provides methods for undoing and
     * redoing the creation of the directory, ensuring that the directory
     * structure and the virtual disk size are updated accordingly.
     * </p>
     */
    static class NewDirCommand implements Command {
        private Directory newDir;
        private Directory dir;
        /**
         * Constructs a NewDirCommand to create a new directory in the specified parent directory.
         *
         * @param newDir the new directory to create
         * @param dir the parent directory in which to create the new directory
         */
        public NewDirCommand(Directory newDir, Directory dir) {
            this.newDir = newDir;
            this.dir = dir;
        }

        @Override
        public void undo() throws Exception {
            dir.removeFile(newDir.getName());
            workingDisk.updateSize();
        }

        @Override
        public void redo() throws Exception {
            dir.addFile(newDir);
            workingDisk.updateSize();
        }
    }
    /**
     * Command to delete a file from a specified directory.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the deletion of a file. It provides methods for undoing and redoing
     * the deletion operation, ensuring that the file can be restored or
     * re-deleted as needed.
     * </p>
     */
    static class DeleteCommand implements Command {
        private File file;
        private Directory dir;
        /**
         * Constructs a DeleteCommand to delete the specified file from the given directory.
         *
         * @param file the file to delete
         * @param dir the directory from which to delete the file
         */
        public DeleteCommand(File file, Directory dir) {
            this.file = file;
            this.dir = dir;
        }

        @Override
        public void undo() throws Exception {
            dir.addFile(file);
            workingDisk.updateSize();
        }

        @Override
        public void redo() throws Exception {
            dir.removeFile(file.getName());
            workingDisk.updateSize();
        }
    }
    /**
     * Command to rename a file.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the renaming of a file. It provides methods for undoing and redoing
     * the rename operation, allowing the file to revert to its previous name
     * or to be renamed again.
     * </p>
     */
    static class RenameCommand implements Command {
        private File file;
        private String prevName;
        private String newName;
        /**
         * Constructs a RenameCommand to rename the specified file.
         *
         * @param file the file to rename
         * @param prevName the previous name of the file before renaming
         */
        public RenameCommand(File file, String prevName) {
            this.file = file;
            this.prevName = prevName;
            this.newName = file.getName();
        }

        @Override
        public void undo() throws Exception {
            file.setName(prevName);
        }

        @Override
        public void redo() throws Exception {
            file.setName(newName);
        }
    }
    /**
     * Command to change the current working directory.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the operation of changing the working directory. It provides methods
     * for undoing and redoing the directory change, allowing the user to
     * revert to the previous directory or switch to a new one.
     * </p>
     */
    static class ChangeDirCommand implements Command {
        private Directory prevDir;
        private Directory newDir;
        /**
         * Constructs a ChangeDirCommand to change the current working directory.
         *
         * @param prevDir the previous working directory
         */
        public ChangeDirCommand(Directory prevDir) {
            this.prevDir = prevDir;
            this.newDir = workingDirectory;
        }

        @Override
        public void undo() throws Exception {
            workingDirectory = prevDir;
        }

        @Override
        public void redo() throws Exception {
            workingDirectory = newDir;
        }
    }
    /**
     * Command to create a new criterion.
     * <p>
     * This class implements the {@link Command} interface and encapsulates
     * the creation of a new criterion. It provides methods for undoing and
     * redoing the addition of the criterion to a collection.
     * </p>
     */
    static class NewCriterionCommand implements Command {
        private String criName;
        private Criterion criterion;
        /**
         * Constructs a NewCriterionCommand to create a new criterion.
         *
         * @param criName the name of the criterion to create
         */
        public NewCriterionCommand(String criName) {
            this.criName = criName;
            this.criterion = criteriaMap.get(criName);
        }

        @Override
        public void undo() throws Exception {
            criteriaMap.remove(criName);
        }

        @Override
        public void redo() throws Exception {
            criteriaMap.put(criName, criterion);
        }
    }

    // Helper method to validate file names
    /**
     * Validates the specified file name against a predefined pattern.
     *
     * @param name the name of the file to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidName(String name) {
        return name != null && name.matches(NAME_PATTERN);
    }
}


