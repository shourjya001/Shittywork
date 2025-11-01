package com.sgcib.iflow.helper.util.impl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class AttachmentUtilImpl {
    
    private static final Logger LOG = LoggerFactory.getLogger(AttachmentUtilImpl.class);
    
    // ADD THESE FIELDS - Spring will inject values from Vault automatically
    @Value("${server.hostname}")
    private String serverHostname;
    
    @Value("${server.username}")
    private String serverUsername;
    
    @Value("${server.password}")
    private String serverPassword;
    
    @Value("${server.port:22}")
    private int serverPort;
    
    @Value("${server.filepath}")
    private String serverFilepath;
    
    /**
     * FIXED: Now uses @Value injected properties instead of manual loading
     */
    private ConnectBean getServerConnectionProperties() {
        ConnectBean connect = new ConnectBean();
        
        try {
            // Use injected fields directly - NO properties.load() needed!
            connect.setUserName(serverUsername);
            connect.setIpAddress(serverHostname);
            connect.setPassword(serverPassword);
            connect.setPort(serverPort);
            
            LOG.info("Server connection configured - Hostname: {}", serverHostname);
            
        } catch (Exception e) {
            LOG.error("Error configuring server connection: {}", e.getMessage());
            e.printStackTrace();
        }
        
        return connect;
    }
    
    /**
     * FIXED: Now uses @Value injected properties
     */
    public Session getFileServerSessions() {
        Session session = null;
        ConnectBean connect = getServerConnectionProperties();
        
        try {
            // ADD THESE LOGS to see what values are being used
            LOG.info("=== SFTP Connection Attempt ===");
            LOG.info("Hostname: {}", connect.getIpAddress());
            LOG.info("Username: {}", connect.getUserName());
            LOG.info("Port: {}", connect.getPort());
            LOG.info("===============================");

            
            JSch jsch = new JSch();
            session = jsch.getSession(connect.getUserName(), connect.getIpAddress(), connect.getPort());
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(connect.getPassword());
            session.connect();
            
            LOG.info("SFTP session established successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to establish SFTP session", e);
            e.printStackTrace();
        }
        
        return session;
    }
    
    /**
     * FIXED: Now uses @Value injected properties
     */
    public String getFilePath(int IflowId) {
        StringBuffer path = null;
        
        try {
            if (IflowId > 0) {
                int folder_path = IflowId / 1000;
                
                // Use injected field directly - NO properties.load() needed!
                path = new StringBuffer()
                    .append(serverFilepath)
                    .append("dmd")
                    .append(String.valueOf(folder_path))
                    .append("K/dmd_")
                    .append(IflowId)
                    .append("/IFL_FORM01_FILE/");
                
                LOG.debug("Generated file path: {}", path.toString());
            }
        } catch (Exception e) {
            LOG.error("<ERROR>[In attachment dao] Reason: {}</ERROR>", e.getMessage());
        }
        
        return path != null ? path.toString() : "";
    }
    
    /**
     * FIXED: Now uses @Value injected properties
     */
    public void saveFile(int iflowid, String stepDetails, String attachmentType, 
                         String fileName, ByteArrayInputStream file, 
                         UserProfileDetails userProfileDetails) {
        
        LOG.info("##########START: Saving file for iflow Id: {}##########", iflowid);
        
        ChannelSftp channelSftp = null;
        Channel channel = null;
        Session session = getFileServerSessions();
        
        try {
            channel = session.openChannel("sftp");
            channel.connect();
            
            if (channel.isConnected()) {
                channelSftp = (ChannelSftp) channel;
                LOG.info("SFTP channel connected successfully");
            }
            
        } catch (JSchException ex) {
            LOG.error("JSchException: {}", ex.getMessage());
            return;
        } catch (Exception ex) {
            LOG.error("Exception: {}", ex.getMessage());
            return;
        }
        
        // Use injected field directly - NO properties.load() needed!
        String path = serverFilepath;
        int folder_path = iflowid / 1000;
        
        String folder1 = "dmd" + folder_path + "K";
        String folder2 = "dmd_" + iflowid;
        String folder3 = getFolderName(stepDetails, attachmentType);
        
        List<String> folderList = new ArrayList<>(Arrays.asList(folder1, folder2, folder3));
        
        try {
            LOG.info("Current directory: {}", channelSftp.pwd());
            channelSftp.cd(path);
            LOG.info("Changed to base directory: {}", channelSftp.pwd());
            
            for (String folder : folderList) {
                if (folder.length() > 0 && !folder.contains(".")) {
                    try {
                        channelSftp.cd(folder);
                        LOG.debug("Changed to existing folder: {}", folder);
                    } catch (SftpException e) {
                        try {
                            // Folder doesn't exist, create it
                            channelSftp.mkdir(folder);
                            // 509 is the octal decimal conversion of 775
                            channelSftp.chmod(509, folder);
                            channelSftp.cd(folder);
                            LOG.info("Created and changed to new folder: {}", folder);
                        } catch (SftpException ex) {
                            LOG.error("Failed to create folder: {}", ex.getMessage());
                        }
                    }
                }
            }
            
            LOG.info("Final path: {}", channelSftp.pwd());
            
        } catch (SftpException ex) {
            LOG.error("SFTP exception during folder navigation: {}", ex.getMessage());
        }
        
        try {
            ByteArrayInputStream inputStream = file;
            
            if (channelSftp != null) {
                channelSftp.put(inputStream, fileName);
                LOG.info("File transfer completed: {}", fileName);
                
                // Make db call to update the file name
                updateFilename(iflowid, stepDetails, fileName, attachmentType, userProfileDetails);
            }
            
        } catch (SftpException e) {
            LOG.error("SFTP Error during file upload", e);
            e.printStackTrace();
            
        } finally {
            if (channelSftp != null) {
                channelSftp.exit();
                LOG.info("SFTP Channel exited.");
            }
            
            if (channel != null) {
                channel.disconnect();
                LOG.info("Channel disconnected.");
            }
            
            if (session != null) {
                session.disconnect();
                LOG.info("Host Session disconnected.");
            }
        }
        
        LOG.info("##########END: Saving file for iflow Id: {}##########", iflowid);
    }
    
    // Your existing helper methods below
    private String getFolderName(String stepDetails, String attachmentType) {
        // Your existing implementation
        return "IFL_FORM01_FILE"; // Example
    }
    
    private void updateFilename(int iflowid, String stepDetails, String fileName, 
                               String attachmentType, UserProfileDetails userProfileDetails) {
        // Your existing database update implementation
        LOG.info("Updating filename in database for iflow: {}", iflowid);
    }
}
