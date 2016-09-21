/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sshdjava.overrride;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessio
 */
public class CustomNativeFileSystemFactory extends NativeFileSystemFactory{

    private static final Logger LOG = LoggerFactory.getLogger(CustomNativeFileSystemFactory.class);
    
    public CustomNativeFileSystemFactory() {
    }

    @Override
    public FileSystemView createFileSystemView(Session session) {
        super.createFileSystemView(session); 
        return new CustomNativeFileSystemView(session.getUsername(),System.getProperty("user.home"),isCaseInsensitive());
    }

    
   
}
