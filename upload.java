// 

She is an exceptionally reliable and detail-focused professional with deep experience in US mortgage bankruptcy processes. Her accuracy, workflow improvements, and leadership in quality review make her an asset to any financial operations team.


Modified upload() method
public ResponseObject upload(
    @ApiParam(name="iflowId",value="Provide the iflowId for which the attachement to be uploaded." + 
        "(ex iflowId:123456)", defaultValue="123456", required=true)
    @RequestParam int iflowId,
    @ApiParam(name="iflowStep", value="Provide the iflow step number in which step the attachement"+
        "to be uploaded. (Valid step numbers are 1,2,5,6, appendix)", defaultValue="1", required=true)
    @RequestParam String iflowStep,
    @ApiParam(name="attachmentType", value="Provide the attchment type counterparty or country when "+
        "the step number is 2 or 6 or appendix", defaultValue="", required=true)
    @RequestParam String attachmentType,
    HttpServletRequest httpRq,
    @RequestParam(value = "attachment", required=true) MultipartFile attachment) {
    
    LOG.info("***********START: UploadAttachmentController.upload()************");
    
    long startTime = System.currentTimeMillis();
    
    // Extract only the filename, not the full path
    String originalFilename = attachment.getOriginalFilename();
    String fileName = originalFilename;
    
    // Remove Windows path
    int lastBackslash = fileName.lastIndexOf('\\');
    if (lastBackslash != -1) {
        fileName = fileName.substring(lastBackslash + 1);
    }
    
    // Remove Unix path
    int lastForwardSlash = fileName.lastIndexOf('/');
    if (lastForwardSlash != -1) {
        fileName = fileName.substring(lastForwardSlash + 1);
    }
    
    int lastDot = fileName.lastIndexOf('.');
    String extension = lastDot != -1 ? fileName.substring(lastDot + 1) : "";
    
    AttachmentDetailsRequest details = new AttachmentDetailsRequest(
        iflowId, 
        iflowStep,
        attachmentType, 
        fileName,
        extension, 
        attachment
    );
    
    ResponseObject response = getService().processRequest(details, attachment);
    
    long endTime = System.currentTimeMillis();
    
    LOG.info("UploadAttachmentController:###########\tWithInitialization time: {} HH:mm:ss.S\n",
        DurationFormatUtils.formatDuration((endTime - startTime), "HH:mm:ss.S"));
    
    LOG.info("***********END: UploadAttachmentController.upload()************");
    
    return response;
}
