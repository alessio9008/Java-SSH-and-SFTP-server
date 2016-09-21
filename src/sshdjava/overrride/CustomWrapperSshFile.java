/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sshdjava.overrride;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.sshd.common.file.SshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessio
 */
public class CustomWrapperSshFile implements SshFile {

    private static final Logger LOG = LoggerFactory.getLogger(CustomWrapperSshFile.class);

    private SshFile sshfile;

    public CustomWrapperSshFile(SshFile sshfile) {
        this.sshfile = sshfile;
    }

    @Override
    public String getAbsolutePath() {
        return sshfile.getAbsolutePath();
    }

    @Override
    public String getName() {
        return sshfile.getName();
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
        try {
            if (sshfile.getAttributes(followLinks) == null) {
                return Collections.emptyMap();
            }
            return sshfile.getAttributes(followLinks);
        } catch (Throwable ex) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
        try {
            sshfile.setAttributes(attributes);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
        return sshfile.getAttribute(attribute, followLinks);
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
        try {
            sshfile.setAttribute(attribute, value);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public String readSymbolicLink() throws IOException {
        return sshfile.readSymbolicLink();
    }

    @Override
    public void createSymbolicLink(SshFile destination) throws IOException {
        sshfile.createSymbolicLink(destination);
    }

    @Override
    public String getOwner() {
        return sshfile.getOwner();
    }

    @Override
    public boolean isDirectory() {
        return sshfile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return sshfile.isFile();
    }

    @Override
    public boolean doesExist() {
        return sshfile.doesExist();
    }

    @Override
    public boolean isReadable() {
        return sshfile.isReadable();
    }

    @Override
    public boolean isWritable() {
        return sshfile.isWritable();
    }

    @Override
    public boolean isExecutable() {
        return sshfile.isExecutable();
    }

    @Override
    public boolean isRemovable() {
        return sshfile.isRemovable();
    }

    @Override
    public SshFile getParentFile() {
        return sshfile.getParentFile();
    }

    @Override
    public long getLastModified() {
        return sshfile.getLastModified();
    }

    @Override
    public boolean setLastModified(long time) {
        return sshfile.setLastModified(time);
    }

    @Override
    public long getSize() {
        return sshfile.getSize();
    }

    @Override
    public boolean mkdir() {
        return sshfile.mkdir();
    }

    @Override
    public boolean delete() {
        return sshfile.delete();
    }

    @Override
    public boolean create() throws IOException {
        return sshfile.create();
    }

    @Override
    public void truncate() throws IOException {
        sshfile.truncate();
    }

    @Override
    public boolean move(SshFile destination) {
        return sshfile.move(destination);
    }

    @Override
    public List<SshFile> listSshFiles() {
        if (sshfile.listSshFiles() == null) {
            return Collections.emptyList();
        }
        return sshfile.listSshFiles();
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        return sshfile.createOutputStream(offset);
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        return sshfile.createInputStream(offset);
    }

    @Override
    public void handleClose() throws IOException {
        sshfile.handleClose();
    }

}
