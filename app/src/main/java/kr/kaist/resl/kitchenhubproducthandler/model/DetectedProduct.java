package kr.kaist.resl.kitchenhubproducthandler.model;

/**
 * Model of detected product
 */

public class DetectedProduct {

    private Integer indicator;
    private String companyPrefix;
    private String itemRefNo;
    private String serial;
    private Integer checksum;
    private Integer containerId;

    public DetectedProduct(Integer indicator, String companyPrefix,
                           String itemRefNo, String serial, Integer checksum,
                           Integer containerId) {
        this.indicator = indicator;
        this.companyPrefix = companyPrefix;
        this.itemRefNo = itemRefNo;
        this.serial = serial;
        this.checksum = checksum;
        this.containerId = containerId;
    }

    public Integer getIndicator() {
        return indicator;
    }

    public void setIndicator(Integer indicator) {
        this.indicator = indicator;
    }

    public String getCompanyPrefix() {
        return companyPrefix;
    }

    public void setCompanyPrefix(String companyPrefix) {
        this.companyPrefix = companyPrefix;
    }

    public String getItemRefNo() {
        return itemRefNo;
    }

    public void setItemRefNo(String itemRefNo) {
        this.itemRefNo = itemRefNo;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public Integer getChecksum() {
        return checksum;
    }

    public void setChecksum(Integer checksum) {
        this.checksum = checksum;
    }

    public Integer getContainerId() {
        return containerId;
    }

    public void setContainerId(Integer containerId) {
        this.containerId = containerId;
    }

}
