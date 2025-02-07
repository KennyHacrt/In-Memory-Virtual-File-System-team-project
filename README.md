# Custom Virtual File System (CVFS)

## Introduction

The Custom Virtual File System (CVFS) is a command-line interface tool that allows users to create, manage, and manipulate virtual disks, directories, and documents.

## Features

- Create and manage virtual disks
- Create, delete, and rename documents and directories
- Search for files based on defined criteria
- Save and load the virtual disk state
- Undo and redo actions

## Commands Overview

### Disk Management
- **newDisk <diskSize>**: Creates a new virtual disk with the specified size.
- **save <path>**: Saves the current virtual disk to a file.
- **load <path>**: Loads a virtual disk from a file.

### File and Directory Management
- **newDoc <docName> <doctype> <docContent>**: Creates a new document.
- **newDir <dirName>**: Creates a new directory.
- **delete <filename>**: Deletes a specified file or directory.
- **rename <oldFileName> <newFileName>**: Renames a file or directory.
- **changeDir <dirName>**: Changes the current working directory.
- **list**: Lists all files and directories in the current directory.
- **rList**: Recursively lists all files and directories.

### Search and Criteria Management
- **newSimpleCri <criName> <attrName> <op> <val>**: Creates a new search criterion.
- **search <criName>**: Searches for files based on a specified criterion.
- **printAllCriteria**: Prints all defined criteria.

### Undo/Redo Management
- **undo**: Reverses the last command executed.
- **redo**: Reapplies the most recently undone command.

### System Control
- **quit**: Terminates the CVFS application.

## Example Workflow

1. Launch CVFS: 
   
   java -jar cvfs.jar
   
2. Create a new disk: 
   
   newDisk 10000
   
3. Add documents and directories, manage files, and define search criteria.

## Troubleshooting

- **Invalid Command Format**: Ensure the correct syntax.
- **Unsupported Document Type**: Use allowed types: txt, java, html, css.
- **Disk Space Exceeded**: Free up space or create a larger disk.

## Conclusion

The CVFS project is designed to simulate a file system in memory, providing users with a robust tool for file management. For more information, refer to the User Manual.
