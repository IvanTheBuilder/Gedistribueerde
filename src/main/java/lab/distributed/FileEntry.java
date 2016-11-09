package lab.distributed;

import java.io.File;

/**
 * Created by licensed on 11/9/2016.
 */
public class FileEntry implements Comparable<FileEntry> {
    private Node owner;
    private Node replicated;
    private Node local;
    private String fileName;
    private Integer hash;

    public FileEntry(String name, Node local, Node owner, Node replicated){
        fileName=name;
        hash=hashName(name);
        this.local=local;
        this.owner=owner;
        this.replicated=replicated;
    }

    public Node getOwner() {
        return owner;
    }

    public Node getReplicated() {
        return replicated;
    }

    public Node getLocal() {
        return local;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getHash() {
        return hash;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        hash=  hashName(fileName);
    }

    public void setReplicated(Node replicated) {
        this.replicated = replicated;
    }

    public void setOwner(Node owner) {
        this.owner = owner;
    }

    /**
     * hash genereren van een bepaalde naam
     *
     * @param name de naam waarvan de hash wordt gegenereerd
     * @return de gegenereerde hash
     */
    private static final int hashName(String name) {
        return Math.abs(name.hashCode() % 32768);
    }

    @Override
    public String toString() {
        return "bestand "+fileName+" staat op node "+owner.getName();
    }

    @Override
    public int compareTo(FileEntry comparable) {
        return hash.compareTo(comparable.getHash());
    }

}
