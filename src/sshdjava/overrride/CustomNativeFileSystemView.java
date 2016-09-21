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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.file.nativefs.NativeSshFile;
import static org.apache.sshd.common.file.nativefs.NativeSshFile.normalizeSeparateChar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessio
 */
public class CustomNativeFileSystemView extends NativeFileSystemView {

    private static final Logger LOG = LoggerFactory.getLogger(CustomNativeFileSystemView.class);

    public CustomNativeFileSystemView(String userName) {
        super(userName);
    }

    public CustomNativeFileSystemView(String userName, boolean caseInsensitive) {
        super(userName, caseInsensitive);
    }

    public CustomNativeFileSystemView(String userName, String current, boolean caseInsensitive) {
        super(userName, getAllRootsOverride(), current, File.separatorChar, caseInsensitive);
    }

    public CustomNativeFileSystemView(String userName, Map<String, String> roots, String current) {
        super(userName, roots, current);
    }

    public CustomNativeFileSystemView(String userName, Map<String, String> roots, String current, char separator, boolean caseInsensitive) {
        super(userName, roots, current, separator, caseInsensitive);
    }

    protected static Map<String, String> getAllRootsOverride() {
        try {
            Method method = searchMethod("getAllRoots");
            method.setAccessible(true);
            return Map.class.cast(MethodHandles.lookup().unreflect(method).invoke());
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    private String appendSlashOverride(String path) {
         try {
            Method method = searchMethod("appendSlash");
            method.setAccessible(true);
            return String.class.cast(MethodHandles.lookup().unreflect(method).invoke(this,path));
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public NativeSshFile createNativeSshFile(String name, File file, String userName) {
        return new CustomWrapperNativeSshFile(super.createNativeSshFile(name, file, userName), this, name, file, userName);
    }

    @Override
    protected SshFile getFile(String dir, String file) {
        return new CustomWrapperSshFile(super.getFile(dir, file)); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        return new CustomWrapperSshFile(super.getFile(baseDir, file)); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SshFile getFile(String file) {
        return new CustomWrapperSshFile(super.getFile(file)); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FileSystemView getNormalizedView() {

        if (getRoots().size() == 1 && getRoots().containsKey("/") && getSeparator() == '/') {
            return this;
        }
        return new CustomNativeFileSystemView(getUserName(), getRoots(), getCurrent(), '/', isCaseInsensitive()) {
            public SshFile getFile(String file) {
                return getFile(reroot(getCurrent()), file);
            }

            public SshFile getFile(SshFile baseDir, String file) {
                return getFile(baseDir.getAbsolutePath(), file);
            }

            public FileSystemView getNormalizedView() {
                return this;
            }

            protected String reroot(String file) {
                if (file == null) {
                    file = ".";
                }
                file = appendSlashOverride(file);
                for (String r : getRoots().keySet()) {
                    if (file.startsWith(r)) {
                        return "/" + normalizeRoot(r) + file.substring(r.length());
                    }
                }
                throw new IllegalArgumentException();
            }

            protected SshFile getFile(String dir, String file) {
                dir = appendSlashOverride(normalizeSeparateChar(dir));
                file = normalizeSeparateChar(file);
                // Compute root + non rooted absolute file
                if (!file.startsWith("/")) {
                    file = dir + file;
                }
                // get actual file object
                String userFileName = NativeSshFile.getPhysicalName("/", "/", file, isCaseInsensitive());
                if (userFileName.equals("/")) {
                    //return rootFile();
                    return new RootFile();
                }
                int idx = userFileName.indexOf("/", 1);
                if (idx < 0) {
                    String root = userFileName + "/";
                    String physRoot = null;
                    for (String r : getRoots().keySet()) {
                        if (("/" + normalizeRoot(r)).equals(root)) {
                            physRoot = getRoots().get(r);
                            break;
                        }
                    }
                    if (physRoot == null) {
                        throw new IllegalArgumentException("Unknown root " + userFileName);
                    }
                    File fileObj = new File(physRoot);
                    userFileName = normalizeSeparateChar(userFileName);
                    return createNativeSshFile(userFileName, fileObj, getUserName());
                } else {
                    String root = userFileName.substring(1, idx) + "/";
                    String physRoot = null;
                    for (String r : getRoots().keySet()) {
                        if (normalizeRoot(r).equals(root)) {
                            physRoot = getRoots().get(r);
                            break;
                        }
                    }
                    if (physRoot == null) {
                        throw new IllegalArgumentException("Unknown root " + userFileName);
                    }
                    File fileObj = new File(physRoot + userFileName.substring(idx + 1));
                    userFileName = normalizeSeparateChar(userFileName);
                    return createNativeSshFile(userFileName, fileObj, getUserName());
                }
            }
        };
    }


    public Boolean isCaseInsensitive() {
        return getSuperclassField("caseInsensitive");
    }

    public Map<String, String> getRoots() {
        return getSuperclassField("roots");
    }

    public String getCurrent() {
        return getSuperclassField("current");
    }

    protected <T> T getSuperclassField(String name) {
        try {
            Field field = searchField(name);
            field.setAccessible(true);
            return (T) MethodHandles.lookup().unreflectGetter(field).invoke(this);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    private static Field searchField(String name) {
        try {
            Class cl = CustomNativeFileSystemView.class;
            while (cl != null) {
                for (Field fi : cl.getDeclaredFields()) {
                    if (fi.getName().equals(name)) {
                        return fi;
                    }
                }
                cl = cl.getSuperclass();
            }

        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    private static Method searchMethod(String name) {
        try {
            Class cl = CustomNativeFileSystemView.class;
            while (cl != null) {
                for (Method mi : cl.getDeclaredMethods()) {
                    if (mi.getName().equals(name)) {
                        return mi;
                    }
                }
                cl = cl.getSuperclass();
            }

        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public char getSeparator() {
        return super.getSeparator(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUserName() {
        return super.getUserName(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setUnsupportedAttributePolicy(UnsupportedAttributePolicy unsupportedAttributePolicy) {
        super.setUnsupportedAttributePolicy(unsupportedAttributePolicy); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UnsupportedAttributePolicy getUnsupportedAttributePolicy() {
        return super.getUnsupportedAttributePolicy(); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    class RootFile implements SshFile {
        public String getAbsolutePath() {
            return "/";
        }
        public String getName() {
            return "/";
        }
        public Map<SshFile.Attribute, Object> getAttributes(boolean followLinks) throws IOException {
            return Collections.emptyMap();
        }
        public void setAttributes(Map<SshFile.Attribute, Object> attributes) throws IOException {
        }
        
        public Object getAttribute(SshFile.Attribute attribute, boolean followLinks) throws IOException {
            return null;
        }
        public void setAttribute(SshFile.Attribute attribute, Object value) throws IOException {
        }
        
        public String readSymbolicLink() throws IOException {
            return null;
        }
        public void createSymbolicLink(SshFile destination) throws IOException {
        }
        
        public String getOwner() {
            return null;
        }
        public boolean isDirectory() {
            return true;
        }
        public boolean isFile() {
            return false;
        }
        public boolean doesExist() {
            return true;
        }
        public boolean isReadable() {
            return true;
        }
        public boolean isWritable() {
            return false;
        }
        public boolean isExecutable() {
            return false;
        }
        public boolean isRemovable() {
            return false;
        }
        public SshFile getParentFile() {
            return null;
        }
        public long getLastModified() {
            return 0;
        }
        public boolean setLastModified(long time) {
            return false;
        }
        public long getSize() {
            return 0;
        }
        public boolean mkdir() {
            return false;
        }
        public boolean delete() {
            return false;
        }
        public boolean create() throws IOException {
            return false;
        }
        public void truncate() throws IOException {
        }
        public boolean move(SshFile destination) {
            return false;
        }
        public List<SshFile> listSshFiles() {
            List<SshFile> list = new ArrayList<SshFile>();
            for (String root : getRoots().keySet()) {
                String display = normalizeRoot(root);
                if(display!=null){
                    display=display.replaceAll(Pattern.quote("/"), "").replaceAll(Pattern.quote("\\"), "").replaceAll(Pattern.quote(":"), "");
                }
                list.add(createNativeSshFile(display, new File(getRoots().get(root)), getUserName()));
            }
            return list;
        }
        public OutputStream createOutputStream(long offset) throws IOException {
            return null;
        }
        public InputStream createInputStream(long offset) throws IOException {
            return null;
        }
        public void handleClose() throws IOException {
        }
    }

}
