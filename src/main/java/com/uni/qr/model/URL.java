package com.uni.qr.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Blob;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
public class URL {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer uid;

    private String description;

    private String vanilla_URL;

    private Blob result;

    private Timestamp timestamp;

    private boolean deleteFlag;


    public Integer getUid() {
        return uid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVanilla_URL() {
        return vanilla_URL;
    }

    public void setVanilla_URL(String vanilla_URL) {
        this.vanilla_URL = vanilla_URL;
    }

    public Blob getResult() {
        return result;
    }

    public void setResult(Blob result) {
        this.result = result;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}
