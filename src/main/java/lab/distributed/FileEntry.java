package lab.distributed;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Een bestandsfiche van een bestand
 * Created by Robrecht on 11/9/2016.
 */
public class FileEntry implements Comparable<FileEntry>, Serializable {
    private String owner;
    private String replicated;
    private String local;
    private String fileName;
    private Integer hash;
    private boolean localIsOwner = false; //dit moet false gezet worden wanneer er een nieuwe node in het netwerk komt en dit niet meer klopt!!!
    private HashSet<String> downloadLocations; // dit is een lijst van ips van de downloadlocaties

    public FileEntry(String name, String local, String owner, String replicated){
        fileName=name;
        hash=hashName(name);
        this.local=local;
        this.owner=owner;
        this.replicated=replicated;
        this.downloadLocations = new HashSet<String>();
        if(replicated!=null)
            downloadLocations.add(replicated);
    }

    public void addDownloadLocation(String IP)
    {
        downloadLocations.add(IP);
        System.out.println("downloadlocaties van "+ fileName + ": "+downloadLocations);
    }

    public boolean removeDownloadLocation(String IP)
    {
        if(downloadLocations.contains(IP))
        {
            downloadLocations.remove(IP);
            return true;
        }else
            return false;
    }

    public HashSet<String> getDownloadLocations()
    {
        return downloadLocations;
    }

    public String getOwner() {
        return owner;
    }

    public String getReplicated() {
        return replicated;
    }

    public String getLocal() {
        return local;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getHash() {
        return hash;
    }

    public boolean getLocalIsOwner() {
        return localIsOwner;
    }

    public void setLocalIsOwner(boolean localIsOwner) {
        this.localIsOwner = localIsOwner;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        hash=  hashName(fileName);
    }

    public void setReplicated(String replicated) {
        this.replicated = replicated;
    }

    public void setOwner(String owner) {
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

    public void putOwner (String owner){
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "bestand "+fileName+" staat op node met hash"+replicated;
    }

    @Override
    public int compareTo(FileEntry comparable) {
        return hash.compareTo(comparable.getHash());
    }

}
