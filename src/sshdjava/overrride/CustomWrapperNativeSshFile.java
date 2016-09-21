/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sshdjava.overrride;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.file.nativefs.NativeSshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessio
 */
public class CustomWrapperNativeSshFile extends NativeSshFile {

    private static final Logger LOG = LoggerFactory.getLogger(CustomWrapperNativeSshFile.class);

    private NativeSshFile nativeSshFile;

    public CustomWrapperNativeSshFile(NativeFileSystemView nativeFileSystemView, String fileName, File file, String userName) {
        super(nativeFileSystemView, fileName, file, userName);
    }

    public CustomWrapperNativeSshFile(NativeSshFile nativeSshFile, NativeFileSystemView nativeFileSystemView, String fileName, File file, String userName) {
        super(nativeFileSystemView, fileName, file, userName);
        this.nativeSshFile = nativeSshFile;
    }

    @Override
    public File getNativeFile() {
        return nativeSshFile.getNativeFile();
    }

    @Override
    public String getAbsolutePath() {
        return nativeSshFile.getAbsolutePath();
    }

    @Override
    public String getName() {
        return nativeSshFile.getName();
    }

    @Override
    public String getOwner() {
        return nativeSshFile.getOwner();
    }

    @Override
    public boolean isDirectory() {
        return nativeSshFile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return nativeSshFile.isFile();
    }

    @Override
    public boolean doesExist() {
        return nativeSshFile.doesExist();
    }

    @Override
    public long getSize() {
        return nativeSshFile.getSize();
    }

    @Override
    public long getLastModified() {
        return nativeSshFile.getLastModified();
    }

    @Override
    public boolean setLastModified(long time) {
        return nativeSshFile.setLastModified(time);
    }

    @Override
    public boolean isReadable() {
        return nativeSshFile.isReadable();
    }

    @Override
    public boolean isWritable() {
        return nativeSshFile.isWritable();
    }

    @Override
    public boolean isExecutable() {
        return nativeSshFile.isExecutable();
    }

    @Override
    public boolean isRemovable() {
        return nativeSshFile.isRemovable();
    }

    @Override
    public SshFile getParentFile() {
        return nativeSshFile.getParentFile();
    }

    @Override
    public boolean delete() {
        return nativeSshFile.delete();
    }

    @Override
    public boolean create() throws IOException {
        return nativeSshFile.create();
    }

    @Override
    public void truncate() throws IOException {
        nativeSshFile.truncate();
    }

    @Override
    public boolean move(SshFile dest) {
        return nativeSshFile.move(dest);
    }

    @Override
    public boolean mkdir() {
        return nativeSshFile.mkdir();
    }

    @Override
    public List<SshFile> listSshFiles() {
        if (nativeSshFile.listSshFiles() == null) {
            return Collections.emptyList();
        }
        return nativeSshFile.listSshFiles();
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        return nativeSshFile.createOutputStream(offset);
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        return nativeSshFile.createInputStream(offset);
    }

    @Override
    public void handleClose() {
        nativeSshFile.handleClose();
    }

    @Override
    public boolean equals(Object obj) {
        return nativeSshFile.equals(obj);
    }

    @Override
    public File getPhysicalFile() {
        return nativeSshFile.getPhysicalFile();
    }

    @Override
    public String toString() {
        return nativeSshFile.toString();
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
        try {
            if (nativeSshFile.getAttributes(followLinks) == null) {
                return Collections.emptyMap();
            }
            return nativeSshFile.getAttributes(followLinks);
        } catch (Throwable ex) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        try {
            nativeSshFile.setAttributes(attributes);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
        return nativeSshFile.getAttribute(attribute, followLinks);
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        try {
            nativeSshFile.setAttribute(attribute, value);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public String readSymbolicLink() throws IOException {
        return nativeSshFile.readSymbolicLink();
    }

    @Override
    public void createSymbolicLink(SshFile destination) throws IOException {
        nativeSshFile.createSymbolicLink(destination);
    }

}
