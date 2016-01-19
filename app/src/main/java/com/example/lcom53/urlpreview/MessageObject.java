package com.example.lcom53.urlpreview;

import java.io.Serializable;

/**
 * @author ParthS
 * @since 19/1/16.
 */
public class MessageObject implements Serializable {
    String domainName;
    String fevicon;
    String TitleDescription;
    String subTitleDescription;

    public String getDomainSnap() {
        return domainSnap;
    }

    public void setDomainSnap(String domainSnap) {
        this.domainSnap = domainSnap;
    }

    public String getSubTitleDescription() {
        return subTitleDescription;
    }

    public void setSubTitleDescription(String subTitleDescription) {
        this.subTitleDescription = subTitleDescription;
    }

    public String getTitleDescription() {
        return TitleDescription;
    }

    public void setTitleDescription(String titleDescription) {
        TitleDescription = titleDescription;
    }

    public String getFevicon() {
        return fevicon;
    }

    public void setFevicon(String fevicon) {
        this.fevicon = fevicon;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    String domainSnap;

}