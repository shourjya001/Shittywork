@Value("${server.hostname}") private String serverHostname;

@Value("${server.username}") private String serverUsername;

@Value("${server.password}") private String serverPassword;

@Value("${server.port:22}") private int serverPort;

private ConnectBean getServerConnectionProperties() { ConnectBean connect = new ConnectBean();

try {   connect.setUserName(serverUsername); connect.setIpAddress(serverHostname); connect.setPassword(serverPassword); connect.setPort(serverPort);

} catch (Exception e) {  System.err.println("Failed to set connection properties: " + e.getMessage()); e.printStackTrace(); throw new RuntimeException("Invalid server configuration", e); }

return connect; }
