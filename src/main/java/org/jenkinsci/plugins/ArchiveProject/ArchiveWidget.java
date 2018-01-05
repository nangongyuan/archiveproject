package org.jenkinsci.plugins.ArchiveProject;

import hudson.widgets.Widget;

import java.util.List;

public class ArchiveWidget extends Widget {

    private List<Backups> backupsList;

    public void setBackupsList(List<Backups> backupsList) {
        this.backupsList = backupsList;
    }

    public List<Backups> getBackupsList() {
        return backupsList;
    }

    public static class Backups {
        private String number;
        private String user;
        private String date;
        private String result;

        public Backups(String number, String name, String duration) {
            this.number = number;
            this.user = user;
            this.date = date;
            this.result = result;
        }

        public Backups() {
            this.number = "0";
            this.user = "anonymous";
            this.date = "1900-01-01";
            this.result = "N/A";
        }

        public String getNumber() {
            return number;
        }

        public String getUser() {
            return user;
        }

        public String getDate() {
            return date;
        }

        public String getResult() {
            return result;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

}